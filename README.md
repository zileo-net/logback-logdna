# LogDNA Logback appender

This library provides an appender for [logback](https://logback.qos.ch), allowing to send your logs to the [LogDNA](https://logdna.com) online logging platform, via their HTTPS Ingest API. MDC thread-bound data can be send, indexed and searched into your LogDNA dashboard.

## How to use it

First, copy this dependency into your `pom.xml` file.

    <dependency>
        <groupId>net.zileo</groupId>
        <artifactId>logback-logdna</artifactId>
        <version>1.0.1</version>
    </dependency>

Note that this library relies on Jersey JAX-RS implementation with Jackson JSON mapper. For a lightweight implementation, you can check [this other appender](https://github.com/robshep/logback-logdna).

Copy the following two LogDna appenders to your `classpath:/logback.xml` file.

    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
    
        <appender name="LogDnaHttp" class="net.zileo.logback.logdna.LogDnaAppender">
            <encoder>
                <pattern>[%thread] %msg%n</pattern>
            </encoder>
            <appName>YouApplicationName</appName>
            <ingestKey>${LOGDNA_INGEST_KEY}</ingestKey>
            <ingestUrl>${LOGDNA_INGEST_URL}</ingestUrl>
            <mdcFields>field1,field2</mdcFields>
            <mdcTypes>string,int</mdcTypes>
            <tags>dev</tags>
        </appender>
        
        <appender name="LogDna" class="ch.qos.logback.classic.AsyncAppender">
            <appender-ref ref="LogDnaHttp" />
            <queueSize>500</queueSize>
            <discardingThreshold>0</discardingThreshold>
            <includeCallerData>false</includeCallerData>
        </appender>
    
        <appender name="Console" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm} %-5level %msg%n</pattern>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="Console" />
            <appender-ref ref="LogDna" />
        </root>
        
    </configuration>
    
This configuration is based on an [asynchronous wrapper](https://logback.qos.ch/manual/appenders.html#AsyncAppender), for performance reason. Check [this nice OverOps blog article](https://blog.takipi.com/how-to-instantly-improve-your-java-logging-with-7-logback-tweaks/) for more info.
    
## Configuration options

* You can use your own pattern.
* Set up your LOGDNA_INGEST_KEY (api key) via the System properties.
* Set up the LOGDNA_INGEST_URL (api url) the same way. (Should be https://logs.logdna.com/logs/ingest)
* Set up comma-separated tags if you want to.
* Set up comma-separated MDC keys to index (from the MDC thread local binding).
* Set up one type for each MDC key.

Possible types are string, boolean, int and long. The last two result in an indexed number in your LogDNA console, which is rather interesting, as it will allow you some functions inside your graphs (sum, average, etc...).

---

Proudly provided by [Zileo.net](https://zileo.net)
