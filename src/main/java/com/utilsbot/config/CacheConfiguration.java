package com.utilsbot.config;

import org.hibernate.cfg.AvailableSettings;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.spring.starter.embedded.InfinispanGlobalConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Bean
    public InfinispanGlobalConfigurer globalConfiguration() {
        log.info("Defining Infinispan Global Configuration");
        return () ->
                GlobalConfigurationBuilder
                        .defaultClusteredBuilder()
                        .transport().defaultTransport()
                        .addProperty("configurationFile", "default-configs/default-jgroups-tcp.xml")
                        .clusterName("infinispan-UtilsBot-cluster")
                        .jmx().disable()
                        .serialization().marshaller(new ProtoStreamMarshaller())
                        .build();
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(CacheFactoryConfiguration cacheFactoryConfiguration) {
        return hibernateProperties -> hibernateProperties.put(AvailableSettings.CACHE_REGION_FACTORY, cacheFactoryConfiguration);
    }
}
