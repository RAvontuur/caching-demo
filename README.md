
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

## logging
add info log message with unique search term "CACHE MISS"\
from distributed logging solution (Kibana, Splunk) obtain statistics\
detect increase in CACHE MISS and create alerts.