package net.zileo.logback.logdna;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import jakarta.ws.rs.ProcessingException;

/**
 * ! One LOGDNA_INGEST_KEY must be set as an environment variable before launching the test !
 * 
 * @author jlannoy
 */
public class LogDnaAppenderIntegrationTest {

    private Logger logger = (Logger) LoggerFactory.getLogger(LogDnaAppenderIntegrationTest.class);

    private LogDnaAppenderDecorator appender;

    @Before
    public void init() throws JoranException {
        LoggerContext loggerContext = ((LoggerContext) LoggerFactory.getILoggerFactory());
        loggerContext.reset();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        configurator.doConfigure("src/test/resources/logback.xml");

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
    public void testInfoLog() {
        MDC.put("requestId", "testInfoLog");
        MDC.put("requestTime", "123");
        this.logger.info("I am Groot");
        isOk();
    }

    @Test
    public void testJsonLog() {
        MDC.put("requestId", "testJsonLog");
        MDC.put("requestTime", "456");
        this.logger.info("I am { name : 'Groot', id : 'GROOT' }");
        isOk();
    }

    @Test
    public void testWarnLog() {
        MDC.put("requestId", "testWarnLog");
        MDC.put("requestTime", "666");
        this.logger.warn("I AM groot");
        isOk();
    }

    @Test
    public void testErrorLog() {
        MDC.put("requestId", "testErrorLog");
        MDC.put("requestTime", "789");
        this.logger.error("I am Groot?", new RuntimeException("GROOT!"));
        isOk();
    }

    @Test
    public void testConnectTimeout() {
        this.appender.setConnectTimeout(1L);
        this.logger.error("I am no Groot");
        assertTrue(appender.hasException());
        assertTrue(ProcessingException.class.isInstance(appender.getException()));
        assertNotNull(appender.getException().getCause());
        assertNotNull(appender.getException().getCause().getMessage());
        assertEquals("connect timed out", appender.getException().getCause().getMessage().toLowerCase());
    }

    @Test
    public void testReadTimeout() {
        this.appender.setReadTimeout(1L);
        this.logger.error("I am no Groot");
        assertTrue(appender.hasException());
        assertTrue(ProcessingException.class.isInstance(appender.getException()));
        assertNotNull(appender.getException().getCause());
        assertEquals("Read timed out", appender.getException().getCause().getMessage());
    }

    private void isOk() {
        if (!appender.isOK() && appender.hasError()) {
            System.out.println(appender.getLogDnaResponse().getStatus() + " - " + appender.getLogDnaResponse().getError());
        }
        if (!appender.isOK() && appender.hasException()) {
            appender.getException().printStackTrace();
        }
        assertTrue(appender.isOK());
    }

    @Ignore
    @Test
    public void testSeveralLogs() {
        for (int i = 1; i <= 30; i++) {
            this.logger.info("I am Groot {}", i);
            if (!appender.isOK() && appender.hasError()) {
                System.out.println(appender.getLogDnaResponse().getStatus() + " - " + appender.getLogDnaResponse().getError());
            }
        }
    }

}