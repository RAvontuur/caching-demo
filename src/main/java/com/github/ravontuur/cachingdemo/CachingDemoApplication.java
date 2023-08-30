package com.github.ravontuur.cachingdemo;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class CachingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CachingDemoApplication.class, args);
    }

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
    private final AtomicInteger finishedCounter = new AtomicInteger();
    @GetMapping("/reset")
    public Boolean reset() {
        log.info("reset finished counter");
        finishedCounter.set(0);
        return true;
    }

    @GetMapping("/requests/count")
    public Integer count() {
        log.info("show counter");
        return finishedCounter.get();
    }

    @GetMapping("/content/{id}")
    public Content content(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "duration", required = false, defaultValue = "1000") Long duration) {
        log.info("start get /content/{}", id);
        Content result = contentService.findContent(id, size, duration);
        log.info("finished get /content/{}, total finished {}", id, finishedCounter.incrementAndGet());
        return result;
    }
}


@Service
@Slf4j
class ContentService {

    @Autowired
    private Database database;

    public Content findContent(long id, int size, long duration) {
        log.info("findContent({},{},{})", id, size, duration);
        return database.executeQuery(id, size, duration);
    }
}

@Service
@Slf4j
class Database {

    public static final int N_DATABASE_THREADS = 20;
    public static final int N_DATABASE_CPU = 20;
    public static final int QUERY_TIMEOUT = 5000; // ms

    ExecutorService executorService = Executors.newFixedThreadPool(N_DATABASE_THREADS);

    private final AtomicInteger competingThreads = new AtomicInteger();

    public Content executeQuery(long id, int size, long duration) {

        competingThreads.incrementAndGet();
        Future<Content> contentFuture = executorService.submit(() -> {
            log.info("executeQuery({},{},{}, competing={})", id, size, duration, competingThreads.get());

            long workToComplete = duration;
            while (workToComplete > 200) {
                workToComplete = workToComplete - workms(200);
            }
            workms(workToComplete);

            return Content.builder()
                    .id(id)
                    .size(size)
                    .duration(duration)
                    .title(String.format("Title %d", id))
                    .body("*".repeat(size))
                    .build();
        });

        try {
            Content result = contentFuture.get(QUERY_TIMEOUT, TimeUnit.MILLISECONDS);
            competingThreads.decrementAndGet();
            return result;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            int competing = competingThreads.getAndDecrement();
            throw new RuntimeException("Failed id=" + id + ", competingThreads=" + competing, e);
        }
    }

    private long workms(long duration) {
        try {
            Thread.sleep(duration * Long.max(1, (competingThreads.get() / N_DATABASE_CPU)));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return duration;
    }
}