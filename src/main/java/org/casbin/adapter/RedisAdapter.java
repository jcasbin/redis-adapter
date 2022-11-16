// Copyright 2021 The casbin Authors. All Rights Reserved.
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

package org.casbin.adapter;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.casbin.adapter.domain.CasbinRule;
import org.casbin.adapter.util.Util;
import org.casbin.jcasbin.model.Assertion;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.BatchAdapter;
import org.casbin.jcasbin.persist.Helper;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * Adapter represents the Redis adapter for policy storage.
 *
 * @author shy
 * @since 2021.04.13
 */
public class RedisAdapter implements Adapter, BatchAdapter{
    private String key;
    private Jedis jedis;

    public RedisAdapter(String host, int port) { newRedisAdapter(host, port, "casbin_rules", null); }

    public RedisAdapter(String host, int port, String password) {
        newRedisAdapter(host, port, "casbin_rules", password);
    }

    public RedisAdapter(String host, int port, String key, String password) {
        newRedisAdapter(host, port, key, password);
    }

    /**
     * loadPolicy loads all policy rules from the storage.
     */
    @Override
    public void loadPolicy(Model model) {
        Long length = jedis.llen(this.key);
        if (length  == null) {
            return;
        }
        List<String> policies = jedis.lrange(this.key, 0, length);
        for (String policy:policies) {
            CasbinRule rule = ((JSONObject) JSONObject.parse(policy)).toJavaObject(CasbinRule.class);
            loadPolicyLine(rule, model);
        }
    }

    /**
     * savePolicy saves all policy rules to the storage.
     */
    @Override
    public void savePolicy(Model model) {
        jedis.del(this.key);
        extracted(model, "p");
        extracted(model, "g");
    }

    /**
     * addPolicy adds a policy rule to the storage.
     */
    @Override
    public void addPolicy(String sec, String ptype, List<String> rule) {
        if (CollectionUtils.isEmpty(rule)) {
            return;
        }
        CasbinRule line = savePolicyLine(ptype, rule);
        jedis.rpush(this.key, JSONObject.toJSONString(line));
    }

    /**
     * removePolicy removes a policy rule from the storage.
     */
    @Override
    public void removePolicy(String sec, String ptype, List<String> rule) {
        if (CollectionUtils.isEmpty(rule)) {
            return;
        }
        CasbinRule line = savePolicyLine(ptype, rule);
        jedis.lrem(this.key, 1, JSONObject.toJSONString(line));
    }

    /**
     * removeFilteredPolicy removes policy rules that match the filter from the storage.
     */
    @Override
    public void removeFilteredPolicy(String sec, String ptype, int fieldIndex, String... fieldValues) {
        List<String> values = Optional.of(Arrays.asList(fieldValues)).orElse(new ArrayList<>());
        if (CollectionUtils.isEmpty(values)) {
            return;
        }

        String regexRule = "";
        for (int i = 0; i < values.size(); ++i) {
            regexRule += "v" + fieldIndex + ":" + values.get(i) + (i + 1 == values.size() ? "" : ",");
            fieldIndex++;
        }
        List<String> rulesMatch = jedis.lrange(this.key, 0, -1);
        jedis.ltrim(this.key, 1, 0);

        String finalRegexRule = ".*" + regexRule + ".*";
        rulesMatch.forEach(rule -> {
            // "{}" is regex symbol in rule lead to regex throw exception, so remove the char

            String tempRule = rule.replaceAll("[\\{ | \\} | \"]", "");
            if (!tempRule.matches(finalRegexRule)) {
                jedis.rpush(this.key, rule);
            }
        });
    }

    /**
     * AddPolicies adds policy rules to the storage.
     */
    @Override
    public void addPolicies(String sec, String ptype, List<List<String>> rules) {
        for (List<String> rule:rules) {
            addPolicy(sec, ptype, rule);
        }
    }

    /**
     * RemovePolicies removes policy rules from the storage.
     */
    @Override
    public void removePolicies(String sec, String ptype, List<List<String>> rules) {
        for (List<String> rule:rules) {
            removePolicy(sec, ptype, rule);
        }
    }

    /**
     * close the redis server
     */
    public void close() {
        jedis.close();
    }

    public void selectDb(int dbIndex) {
        if(jedis != null) {
            jedis.select(dbIndex);
        }
    }

    private void newRedisAdapter(String host, int port, String key, String password) {
        this.key = key;

        jedis = new Jedis(host, port);
        if (password != null) {
            jedis.auth(password);
        }

        Util.logPrintf("Redis service is running ", jedis.ping());
    }

    private void extracted(Model model, String type) {
        for (Map.Entry<String, Assertion> entry : model.model.get(type).entrySet()) {
            String ptype = entry.getKey();
            Assertion ast = entry.getValue();

            for (List<String> rule : ast.policy) {
                CasbinRule line = savePolicyLine(ptype, rule);
                jedis.rpush(this.key, JSONObject.toJSONString(line));
            }
        }
    }

    private void loadPolicyLine(CasbinRule line, Model model) {
        String lineText = line.getPtype();
        if (!"".equals(line.getV0()) && line.getV0() != null) {
            lineText += ", " + line.getV0();
        }
        if (!"".equals(line.getV1()) && line.getV1() != null) {
            lineText += ", " + line.getV1();
        }
        if (!"".equals(line.getV2()) && line.getV2() != null) {
            lineText += ", " + line.getV2();
        }
        if (!"".equals(line.getV3()) && line.getV3() != null) {
            lineText += ", " + line.getV3();
        }
        if (!"".equals(line.getV4()) && line.getV4() != null) {
            lineText += ", " + line.getV4();
        }
        if (!"".equals(line.getV5()) && line.getV5() != null) {
            lineText += ", " + line.getV5();
        }

        Helper.loadPolicyLine(lineText, model);
    }

    private CasbinRule savePolicyLine(String ptype, List<String> rule) {
        CasbinRule line = new CasbinRule();

        line.setPtype(ptype);
        if (rule.size() > 0) {
            line.setV0(rule.get(0));
        }
        if (rule.size() > 1) {
            line.setV1(rule.get(1));
        }
        if (rule.size() > 2) {
            line.setV2(rule.get(2));
        }
        if (rule.size() > 3) {
            line.setV3(rule.get(3));
        }
        if (rule.size() > 4) {
            line.setV4(rule.get(4));
        }
        if (rule.size() > 5) {
            line.setV5(rule.get(5));
        }

        return line;
    }
}
