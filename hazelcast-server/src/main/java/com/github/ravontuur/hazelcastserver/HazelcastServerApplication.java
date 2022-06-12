package com.github.ravontuur.hazelcastserver;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Slf4j
public class HazelcastServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HazelcastServerApplication.class, args);
    }

    @Configuration
    class HazelcastConfig {
        @Bean
        HazelcastInstance hazelcastInstance() {
            return Hazelcast.newHazelcastInstance();
        }
    }
}
