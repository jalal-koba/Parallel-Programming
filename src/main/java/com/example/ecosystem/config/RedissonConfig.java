package com.example.ecosystem.config;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Value("${app.redis.enabled:true}")
    private boolean redisEnabled;

    @Bean
    public RedissonClient redissonClient() {
        if (!redisEnabled) {
            return createDummyRedissonClient();
        }
        try {
            Config config = new Config();
            String redisAddress = String.format("redis://%s:%s", redisHost, redisPort);
            config.useSingleServer()
                  .setAddress(redisAddress);
            return Redisson.create(config);
        } catch (Exception e) {
            System.err.println("Failed to initialize real RedissonClient, falling back to dummy: " + e.getMessage());
            return createDummyRedissonClient();
        }
    }

    private RedissonClient createDummyRedissonClient() {
        return (RedissonClient) java.lang.reflect.Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(),
                new Class<?>[]{RedissonClient.class},
                (proxy, method, args) -> {
                    if ("getLock".equals(method.getName())) {
                        return createDummyLock();
                    }
                    if ("shutdown".equals(method.getName())) {
                        return null;
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "DummyRedissonClient";
                    }
                    return null;
                }
        );
    }

    private RLock createDummyLock() {
        return (RLock) java.lang.reflect.Proxy.newProxyInstance(
                RLock.class.getClassLoader(),
                new Class<?>[]{RLock.class},
                (proxy, method, args) -> {
                    if ("tryLock".equals(method.getName())) {
                        return true;
                    }
                    if ("isHeldByCurrentThread".equals(method.getName())) {
                        return true;
                    }
                    if ("unlock".equals(method.getName())) {
                        return null;
                    }
                    if ("lock".equals(method.getName())) {
                        return null;
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "DummyLock";
                    }
                    return null;
                }
        );
    }
}

