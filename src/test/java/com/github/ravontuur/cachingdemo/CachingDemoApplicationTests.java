package com.github.ravontuur.cachingdemo;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
class CachingDemoApplicationTests {

    private WebClient webClient;
    private WebClient webClientCounter;

    private final AtomicLong totalNumberOfRequests = new AtomicLong(0L);
    private final AtomicLong totalDuration = new AtomicLong(0L);

    private WebClient createWebClient(long responseTimeout, long port) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofMillis(responseTimeout)))
                )
                .baseUrl("http://localhost:" + port).build();
    }

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
            totalNumberOfRequests.incrementAndGet();
            totalDuration.addAndGet(System.currentTimeMillis() - startTime);
            log.info("received call {} with id {}", callId, id);
            return result;
        }
    }

    @Test
    void testWithLoad() throws InterruptedException {

        // load
        int nCalls = 200;
        int nThreads = 2;
        int idOffset = 0; // start range of ids
        int idMod = 1; // max number of ids (recycle)

        // client side
        long port = 8080;
        long responseTimeoutClient = 10000; //ms

        // server side
        // see application.properties for server.tomcat.threads.max
        int durationQuery = 1000; // ms
        int bodySize = 5000; // bytes

        // other
        long timeoutLoadTest = 100000; // ms
        long sleepBeforeCountStats = 1000; //ms
        long responseTimeoutCountStats = 20000; //ms

        webClient = createWebClient(responseTimeoutClient, port);
        webClientCounter = createWebClient(responseTimeoutCountStats, port);

        log.info("started testWithLoad");
        assertThat(webClient.get().uri("/reset").retrieve().bodyToMono(Boolean.class).block()).isTrue();

        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

        Collection<TestCallable> callables = new ArrayList<>();

        for (int callId = 0; callId < nCalls; callId++) {
            long id = idOffset + callId % idMod;
            callables.add(new TestCallable(callId, id, durationQuery, bodySize));
        }
        log.info("Created {} callables", callables.size());
        // invoke all waits until all futures are completed or timeout expired
        List<Future<Content>> futures = executorService.invokeAll(callables, timeoutLoadTest, TimeUnit.MILLISECONDS);
        log.info("Invoked all {} callables", callables.size());

        log.info("Check successful completion");
        int nCancelled = 0;
        int nExecutionExceptions = 0;
        for (int callId = 0; callId < nCalls; callId++) {
            Future<Content> future = futures.get(callId);
            if (future.isCancelled()) {
                nCancelled++;
                continue;
            }
            try {
                Content result = future.get();
                assertThat(result).isNotNull();
            } catch (ExecutionException e) {
                if (e.getCause().getCause() instanceof ReadTimeoutException) {
                    log.info("Execution exception: ReadTimeoutException");
                } else {
                    log.info("Execution exception:", e.getCause());
                }
                nExecutionExceptions++;
            }
        }

        int nSuccessful = nCalls - nCancelled - nExecutionExceptions;
        log.info("Successful calls: {}, Cancelled calls: {}, Execution exceptions: {}",
                nSuccessful, nCancelled, nExecutionExceptions);
        if (totalNumberOfRequests.get() == 0) {
            log.info("Not any call with measured duration completed");
        } else {
            log.info("Average duration calls: {} ms", totalDuration.get() / totalNumberOfRequests.get());
        }

        Thread.sleep(sleepBeforeCountStats);
        Long count = webClientCounter.get().uri("/requests/count").retrieve().bodyToMono(Long.class).block();
        log.info("Number of requests handled by service: {}", count);
    }
}
