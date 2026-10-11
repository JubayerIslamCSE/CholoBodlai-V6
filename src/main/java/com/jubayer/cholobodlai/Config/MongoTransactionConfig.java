
package com.jubayer.cholobodlai.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

@Configuration
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager transactionManager(
            MongoDatabaseFactory factory) {

        return new MongoTransactionManager(factory);
    }
}
