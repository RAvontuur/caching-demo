package com.github.ravontuur.cachingdemo;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class CachingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CachingDemoApplication.class, args);
    }

}
@Configuration
@EnableCaching
class CacheConfig {

}

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Content {
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
    private CacheManager cacheManager;

    private final AtomicInteger requestCounter = new AtomicInteger();

    @GetMapping("/reset")
    public Boolean reset() {
        log.info("reset request counter");
        requestCounter.set(0);
        return true;
    }

    @GetMapping("/requests/count")
    public Long requestsCount() {
        long count = requestCounter.get();
        log.info("total requests {}", count);
        return count;
    }

    @GetMapping("/content/{id}")
    public Content content(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "duration", required = false, defaultValue = "1000") Long duration) {
        log.debug("start get /content/{}", id);
        Content result = contentService.findContent(id, size, duration);
        log.debug("finished get /content/{}, total finished {}", id, requestCounter.incrementAndGet());
        return result;
    }

    @GetMapping("/cacheinfo")
    public String cacheinfo() {
        return cacheManager.getCacheNames().toString();
    }
}


@Service
@Slf4j
class ContentService {

    @Cacheable(value = "content", key = "#id")
    public Content findContent(long id, int size, long duration) {
        log.info("CACHE MISS: findContent({},{},{})",id, size, duration);
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
}