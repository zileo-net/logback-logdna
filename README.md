# LogDNA Logback appender

This library provides an appender for [logback](https://logback.qos.ch), allowing to send your logs to the [LogDNA](https://logdna.com) online logging platform, via their HTTPS Ingest API. MDC thread-bound data can be send, indexed and searched into your LogDNA dashboard.

## How to use it

First, copy this dependency into your `pom.xml` file (if your using Maven).

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
