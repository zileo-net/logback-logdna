package net.zileo.logback.logdna;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class LogDnaAppenderIntegrationTest {

    @Test
    public void testInfoLog() throws InterruptedException {
        MDC.put("requestId", "testInfoLog");
        MDC.put("requestTime", "123");

        Logger logger = LoggerFactory.getLogger(LogDnaAppenderIntegrationTest.class);
        logger.info("I am Groot");

        Thread.sleep(2000);
    }

    @Test
    public void testJsonLog() throws InterruptedException {
        MDC.put("requestId", "testJsonLog");
        MDC.put("requestTime", "456");

        Logger logger = LoggerFactory.getLogger(LogDnaAppenderIntegrationTest.class);
        logger.info("I am { name : 'Groot', id : 'GROOT' }");

        Thread.sleep(2000);
    }

    @Test
    public void testErrorLog() throws InterruptedException {
        MDC.put("requestId", "testErrorLog");
        MDC.put("requestTime", "789");

        Logger logger = LoggerFactory.getLogger(LogDnaAppenderIntegrationTest.class);
        logger.error("I am Groot?", new RuntimeException("GROOT!"));

        Thread.sleep(2000);
    }

}