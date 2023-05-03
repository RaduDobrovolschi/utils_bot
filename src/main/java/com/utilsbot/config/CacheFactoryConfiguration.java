package com.utilsbot.config;

import org.hibernate.service.ServiceRegistry;
import org.infinispan.hibernate.cache.v60.InfinispanRegionFactory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class CacheFactoryConfiguration extends InfinispanRegionFactory {
    private final EmbeddedCacheManager cacheManager;

    public CacheFactoryConfiguration(EmbeddedCacheManager cacheManager) {
        super();
        this.cacheManager = cacheManager;
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties, ServiceRegistry serviceRegistry) {
        return cacheManager;
    }
}
