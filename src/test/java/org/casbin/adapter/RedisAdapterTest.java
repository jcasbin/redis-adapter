package org.casbin.adapter;// Copyright 2021 The casbin Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import org.casbin.jcasbin.main.Enforcer;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RedisAdapterTest {

    private String host = "localhost";
    private int port = 6379;
    private RedisAdapter redisAdapter;

    @Test
    public void testAdapter() {
        Enforcer enforcer;
        // Because the DB is empty at first, so we need to load the policy from the file adapter (.CSV) first.
        enforcer = new Enforcer("examples/rbac_model.conf", "examples/rbac_policy.csv");
        redisAdapter = new RedisAdapter(host, port);

        // This is a trick to save the current policy to the DB.
        // We can't call e.SavePolicy() because the adapter in the enforcer is still the file adapter.
        // The current policy means the policy in the Casbin enforcer (aka in memory).
        redisAdapter.savePolicy(enforcer.getModel());

        // Clear the current policy.
        enforcer.clearPolicy();
        testGetPolicy(enforcer, new ArrayList<>());

        // Load the policy from DB.
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(Arrays.asList("alice", "data1", "read"),
                Arrays.asList("bob", "data2", "write"),
                Arrays.asList("data2_admin", "data2", "read"),
                Arrays.asList("data2_admin", "data2", "write")));

        // Now the DB has policy, so we can provide a normal use case.
        // Create an adapter and an enforcer.
        // NewEnforcer() will load the policy automatically.
        testGetPolicy(enforcer, Arrays.asList(Arrays.asList("alice", "data1", "read"),
                Arrays.asList("bob", "data2", "write"),
                Arrays.asList("data2_admin", "data2", "read"),
                Arrays.asList("data2_admin", "data2", "write")));
        // Add one policy to DB
        redisAdapter.addPolicy("p", "p", Arrays.asList("paul", "data2", "read"));
        enforcer.clearPolicy();
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(Arrays.asList("alice", "data1", "read"),
                Arrays.asList("bob", "data2", "write"),
                Arrays.asList("data2_admin", "data2", "read"),
                Arrays.asList("data2_admin", "data2", "write"),
                Arrays.asList("paul", "data2", "read")));
        // Remove one policy from DB
        redisAdapter.removePolicy("p", "p", Arrays.asList("paul", "data2", "read"));
        enforcer.clearPolicy();;
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(Arrays.asList("alice", "data1", "read"),
                Arrays.asList("bob", "data2", "write"),
                Arrays.asList("data2_admin", "data2", "read"),
                Arrays.asList("data2_admin", "data2", "write")));

        // Add policies to DB
        redisAdapter.addPolicies("p", "p", Arrays.asList(Arrays.asList("curry", "data1", "write"),
                Arrays.asList("kobe", "data2", "read")));
        enforcer.clearPolicy();
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(Arrays.asList("alice", "data1", "read"),
                Arrays.asList("bob", "data2", "write"),
                Arrays.asList("data2_admin", "data2", "read"),
                Arrays.asList("data2_admin", "data2", "write"),
                Arrays.asList("curry", "data1", "write"),
                Arrays.asList("kobe", "data2", "read")));

        // Remove polices from DB
        redisAdapter.removePolicies("p", "p", Arrays.asList(Arrays.asList("curry", "data1", "write"),
                Arrays.asList("kobe", "data2", "read")));
        enforcer.clearPolicy();
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(Arrays.asList("alice", "data1", "read"),
                Arrays.asList("bob", "data2", "write"),
                Arrays.asList("data2_admin", "data2", "read"),
                Arrays.asList("data2_admin", "data2", "write")));

        redisAdapter.close();
    }

    @Test
    public void testRemoveFilteredPolicy() {
        redisAdapter = new RedisAdapter(host, port);
        Enforcer enforcer = new Enforcer("examples/rbac_model.conf", redisAdapter);

        enforcer.clearPolicy();
        redisAdapter.savePolicy(enforcer.getModel());

        enforcer.addPolicies(Arrays.asList(
                Arrays.asList("alice", "data1", "write"),
                Arrays.asList("alice", "data1", "read"),
                Arrays.asList("alice", "data2", "read"),
                Arrays.asList("alice", "data2", "write"),
                Arrays.asList("bob", "data1", "write"),
                Arrays.asList("bob", "data1", "read"),
                Arrays.asList("bob", "data2", "read"),
                Arrays.asList("bob", "data2", "write")));

        enforcer.removeFilteredPolicy(1, "data1", "read");
        enforcer.clearPolicy();
        enforcer.loadPolicy();
        testGetPolicy(enforcer, Arrays.asList(
                Arrays.asList("alice", "data1", "write"),
                Arrays.asList("alice", "data2", "read"),
                Arrays.asList("alice", "data2", "write"),
                Arrays.asList("bob", "data1", "write"),
                Arrays.asList("bob", "data2", "read"),
                Arrays.asList("bob", "data2", "write")));

        enforcer.removeFilteredPolicy(0, "alice");
        enforcer.clearPolicy();
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(
                Arrays.asList("bob", "data1", "write"),
                Arrays.asList("bob", "data2", "read"),
                Arrays.asList("bob", "data2", "write")));

        enforcer.removeFilteredPolicy(0, "bob", "data1", "write");
        enforcer.clearPolicy();
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(
                Arrays.asList("bob", "data2", "read"),
                Arrays.asList("bob", "data2", "write")));

        enforcer.removeFilteredPolicy(2, "read");
        enforcer.clearPolicy();
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(
                Arrays.asList("bob", "data2", "write")));

        enforcer.removeFilteredPolicy(1, "data2");
        enforcer.clearPolicy();
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList());
    }

    @Test
    public void testSelectDb() {
        Enforcer enforcer = new Enforcer("examples/rbac_model.conf", "examples/rbac_policy.csv");
        redisAdapter = new RedisAdapter(host, port);
        // save policy to db 0
        redisAdapter.savePolicy(enforcer.getModel());
        // select db to 1
        redisAdapter.selectDb(1);
        // add policy to db 1
        redisAdapter.addPolicy("p", "p", Arrays.asList("paul", "data2", "read"));
        enforcer.clearPolicy();
        redisAdapter.loadPolicy(enforcer.getModel());
        testGetPolicy(enforcer, Arrays.asList(Arrays.asList("paul", "data2", "read")));
    }

    private void testGetPolicy(Enforcer e, List<List<String>> res) {
        List<List<String>> policies = e.getPolicy();
        Assert.assertEquals(res, policies);
    }
}
