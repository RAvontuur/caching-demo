package com.github.ravontuur.cachingdemo;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
public class CachingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CachingDemoApplication.class, args);
    }

}

@Configuration
@EnableCaching
@Slf4j
class CacheConfig {

    @Bean
    ClientConfig clientConfig() {
        ClientConfig clientConfig = new ClientConfig();

        NearCacheConfig nearCacheConfig = new NearCacheConfig();

        clientConfig.addNearCacheConfig(nearCacheConfig);
        return clientConfig;
    }

}

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Content implements Serializable {
    private Long id;
    private Integer size;
    private Long duration;
    private String title;
    private String body;
}

@RestController
@Slf4j
class ContentController {

    @Autowired
    private ContentService contentService;

    @Autowired
    HazelcastCacheManager hazelcastCacheManager;

    private final AtomicInteger requestCounter = new AtomicInteger();
    private final AtomicLong durationCounterNanos = new AtomicLong();

    @GetMapping("/reset")
    public Boolean reset() {
        log.info("reset request/duration counters");
        requestCounter.set(0);
        durationCounterNanos.set(0L);
        return true;
    }

    @GetMapping("/requests/count")
    public Long requestsCount() {
        long count = requestCounter.get();
        long duration = durationCounterNanos.get();
        log.info("total requests {}, avg duration {} nanos", count, duration/count);
        return count;
    }

    @GetMapping("/content/{id}")
    public Content content(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "duration", required = false, defaultValue = "1000") Long duration) {
        log.debug("start get /content/{}", id);
        long startTime = System.nanoTime();
        Content result = contentService.findContent(id, size, duration);
        durationCounterNanos.addAndGet(System.nanoTime() - startTime);
        log.debug("finished get /content/{}, total finished {}", id, requestCounter.incrementAndGet());
        return result;
    }

    @GetMapping("/content/evict/{id}")
    public String evict(@PathVariable(value = "id") Long id) {
        contentService.cacheEvict(id);
        return "evicted: " + id;
    }

    @GetMapping("/cacheinfo")
    public String cacheinfo() {
        return hazelcastCacheManager.getCacheNames().toString();
    }
}


@Service
@Slf4j
class ContentService {

    @Cacheable(value = "content", key = "#id", sync = true)
    public Content findContent(long id, int size, long duration) {
        log.info("CACHE MISS: findContent({},{},{})", id, size, duration);

        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Content.builder()
                .id(id)
                .size(size)
                .duration(duration)
                .title(String.format("Title %d", id))
                .body("*".repeat(size))
                .build();
    }

    @CacheEvict(value = "content", key = "#id")
    public void cacheEvict(Long id) {
        log.info("CACHE EVICT id={}", id);
    }
}