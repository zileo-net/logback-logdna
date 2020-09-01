package net.zileo.logback.logdna;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.ProcessingException;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * ! One LOGDNA_INGEST_KEY must be set as an environment variable before launching the test !
 * 
 * @author jlannoy
 */
public class LogDnaAppenderIntegrationTest {

    private Logger logger = (Logger) LoggerFactory.getLogger(LogDnaAppenderIntegrationTest.class);

    private LogDnaAppenderDecorator appender;

    @Before
    public void setUp() {
        this.appender = new LogDnaAppenderDecorator();
        this.appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        this.appender.setIngestUrl("https://logs.logdna.com/logs/ingest");
        this.appender.setIngestKey(System.getenv("LOGDNA_INGEST_KEY"));
        this.appender.start();

        this.logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        this.logger.detachAppender(appender);
    }

    @Test
    public void testNoCredentials() {
        this.appender.headers.remove("apikey");
        this.logger.error("I am no Groot");
        hasError("Missing Credentials", 401);
    }

    @Test
    public void testInfoLog() {
        MDC.put("requestId", "testInfoLog");
        MDC.put("requestTime", "123");
        this.logger.info("I am Groot");
        assertTrue(appender.isOK());

    }

    @Test
    public void testJsonLog() {
        MDC.put("requestId", "testJsonLog");
        MDC.put("requestTime", "456");
        this.logger.info("I am { name : 'Groot', id : 'GROOT' }");
        assertTrue(appender.isOK());
    }

    @Test
    public void testWarnLog() {
        MDC.put("requestId", "testWarnLog");
        MDC.put("requestTime", "666");
        this.logger.warn("I AM groot");
        assertTrue(appender.isOK());
    }

    @Test
    public void testErrorLog() {
        MDC.put("requestId", "testErrorLog");
        MDC.put("requestTime", "789");
        this.logger.error("I am Groot?", new RuntimeException("GROOT!"));
        assertTrue(appender.isOK());
    }

    @Test
    public void testConnectTimeout() {
        this.appender.setConnectTimeout(2L);
        this.logger.error("I am no Groot");
        assertTrue(appender.hasException());
        assertTrue(ProcessingException.class.isInstance(appender.getException()));
        assertNotNull(appender.getException().getCause());
        assertEquals("connect timed out", appender.getException().getCause().getMessage());
    }

    @Test
    public void testReadTimeout() {
        this.appender.setReadTimeout(2L);
        this.logger.error("I am no Groot");
        assertTrue(appender.hasException());
        assertTrue(ProcessingException.class.isInstance(appender.getException()));
        assertNotNull(appender.getException().getCause());
        assertEquals("Read timed out", appender.getException().getCause().getMessage());
    }

    private void hasError(String message, int statusCode) {
        assertFalse(appender.hasException());
        assertFalse(appender.isOK());
        assertTrue(appender.hasError());
        assertEquals(message, appender.getLogDnaResponse().getError());
        assertEquals(statusCode, appender.getResponse().getStatus());
    }

}