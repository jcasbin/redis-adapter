# Redis Adapter 

[![build](https://github.com/jcasbin/redis-adapter/actions/workflows/maven-ci.yml/badge.svg)](https://github.com/jcasbin/redis-adapter/actions)
[![codebeat badge](https://codebeat.co/badges/560a67fc-53b6-4a10-8e1b-989f3bb4e5cb)](https://codebeat.co/projects/github-com-jcasbin-redis-adapter-master)
[![codecov](https://codecov.io/gh/jcasbin/redis-adapter/branch/master/graph/badge.svg?token=5wzDaTC9UV)](https://codecov.io/gh/jcasbin/redis-adapter)
[![Javadocs](https://www.javadoc.io/badge/org.casbin/redis-adapter.svg)](https://www.javadoc.io/doc/org.casbin/redis-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/org.casbin/redis-adapter.svg)](https://mvnrepository.com/artifact/org.casbin/redis-adapter/latest)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/casbin/lobby)

Redis Adapter is the [Redis](https://redis.io/) adapter for [jCasbin](https://github.com/casbin/jcasbin). With this library, Casbin can load policy from Redis or save policy to it.

## Installation

```xml
    <dependency>
        <groupId>org.casbin</groupId>
        <artifactId>redis-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
```

## Simple Example

```java
package org.casbin.adapter;

import org.casbin.jcasbin.main.Enforcer;

public class Main {
    public static void main(String[] args) {
        // Initialize a Redis adapter and use it in a jCasbin enforcer:
        RedisAdapter a = new RedisAdapter("localhost", 6379);
        // Use the following if Redis has password like "123"
        // RedisAdapter a = new RedisAdapter("localhost", 6379, "123");
        Enforcer e = new Enforcer("examples/rbac_model.conf", a);

        // Load the policy from DB.
        e.loadPolicy();

        // Check the permission.
        e.enforce("alice", "data1", "read");

        // Modify the policy.
        // e.addPolicy(...);
        // e.removePolicy(...);

        // Save the policy back to DB.
        e.savePolicy();
    }
}
```

## Getting Help

- [jCasbin](https://github.com/casbin/jcasbin)

## License

This project is under Apache 2.0 License. See the [LICENSE](LICENSE) file for the full license text.
