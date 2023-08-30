
# Spring Caching
https://docs.spring.io/spring-boot/docs/2.1.6.RELEASE/reference/html/boot-features-caching.html#boot-features-caching

Ehcache version 3 provides an implementation of a JSR-107 cache manager.

**Cache hit ratio** is a measurement of how many content requests a cache is able to fill successfully, 
compared to how many requests it receives.

# Spring Actuator

https://docs.spring.io/spring-boot/docs/current/actuator-api/htmlsingle/

http://localhost:8080/actuator/beans
http://localhost:8080/actuator/caches
http://localhost:8080/actuator/caches/content

http://localhost:8080/actuator/metrics
http://localhost:8080/actuator/metrics/jvm.memory.used

EXERCISE: add cache metrics

## cache eviction via actuator
curl 'http://localhost:8080/actuator/caches' -i -X DELETE

curl 'http://localhost:8080/actuator/caches/content?cacheManager=cacheManager' -i -X DELETE

(repeat for each running instance of your service)

## open source
why?
- knowledge training
- for evaluation cache solutions
- production issue solving

Show how to debug.\
\
interesting findings:
- dependency on ehcache not needed (Spring uses its own implementation)
- webclient is cancelled, but requests still being handled by service application

EXERCISE: migrate to using ehCache (incl. ehcache.xml , eg TTL)

# logging
add info log message with unique search term "CACHE MISS"\
from distributed logging solution (Kibana, Splunk) obtain statistics\
detect increase in CACHE MISS and create alerts.

# cache eviction

example:\
http://localhost:8080/content/evict/1

(repeat for each running instance of your service)

# thundering herd issue
look at the application log of service\
observe unexpected cache misses\
\
solution: `sync = true`
\
The use of `sync = true` has some limitations. See Spring documentation.

# Hazelcast

- do not forget to include hazelcast.com:hazelcast-spring
- restart all Hazelcast server instances to evict all entries
- cache eviction will be propagated top all instances (including near caches)
- new release service instance: ensure compatibility new version of Content-class
- Hazelcast has a near cache with microsecond-level performance.\
Do not place @Cacheable in microservices, but in its clients.
- deploy Hazelcast Management Center (without license up to two Hazelcast server instances)

EXERCISE: test with two instances of Hazelcast Server, and observe\
replication behavior in Hazelcast Management Center.
https://docs.hazelcast.com/management-center/4.2022.01/getting-started

`java -Dhazelcast.mc.http.port=8083 -jar hazelcast-management-center-*.jar`

or:
`cd Downloads/hazelcast-management-center-5.3.2/bin`
`./start.sh 8180`

EXERCISE: caching named lists of Content-items.\
How to evict lists if a Content-item changes?

EXERCISE: use of @CachePut

