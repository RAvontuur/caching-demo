package com.github.ravontuur.cachingdemo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
class CachingDemoApplicationTests {

    private WebClient webClient = WebClient.builder().baseUrl("http://localhost:8080").build();

    private final AtomicLong totalDuration = new AtomicLong(0L);

    @AllArgsConstructor
    class TestCallable implements Callable<Content> {

        private final Integer callId;
        private final Long id;
        private final Integer durationQuery;
        private final Integer bodySize;

        @Override
        public Content call() {
            long startTime = System.currentTimeMillis();
            Content result = webClient.get().uri(
                    "/content/" + id + "?size=" + bodySize + "&duration=" + durationQuery)
                    .retrieve().bodyToMono(Content.class).block();
            totalDuration.addAndGet(System.currentTimeMillis() - startTime);
            log.info("received call {} with id {}", callId, id);
            return result;
        }
    }

    @Test
    void testWithLoad() throws InterruptedException, ExecutionException {

        int nCalls = 100;
        int nThreads = 100;
        long timeoutClient = 10000; // ms
        int durationQuery = 2000; // ms
        int bodySize = 5;

        log.info("started testWithLoad");
        assertThat(webClient.get().uri("/reset").retrieve().bodyToMono(Boolean.class).block()).isTrue();

        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

        Collection<TestCallable> callables = new ArrayList<>();

        for (int callId = 0; callId < nCalls; callId++) {
            long id = callId;
            callables.add(new TestCallable(callId, id, durationQuery, bodySize));
        }
        log.info("Created {} callables", callables.size());
        // invoke all waits until all futures are completed or timeout expired
        List<Future<Content>> futures = executorService.invokeAll(callables, timeoutClient, TimeUnit.MILLISECONDS);
        log.info("Invoked all {} callables", callables.size());

        log.info("Check successful completion");
        int nCancelled = 0;
        for (int callId = 0; callId < nCalls; callId++) {
            Future<Content> future = futures.get(callId);
            if (future.isCancelled()) {
                nCancelled++;
                continue;
            }
            Content result = future.get();

            assertThat(result).isNotNull();
        }

        int nSuccessful = nCalls - nCancelled;
        log.info("Successful calls: {}, Cancelled calls: {}", nSuccessful, nCancelled);
        log.info("Query duration: {} ms", durationQuery);
        log.info("Average duration successful calls: {} ms", totalDuration.get() / nSuccessful);
    }
}
