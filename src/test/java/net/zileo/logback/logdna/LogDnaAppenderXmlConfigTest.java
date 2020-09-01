package net.zileo.logback.logdna;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;

/**
 * ! LOGDNA_INGEST_KEY and LOGDNA_INGEST_URL must be set as an environment variable before launching the test !
 * 
 * @author jlannoy
 */
public class LogDnaAppenderXmlConfigTest {

    static {
        System.setProperty("xmlConfig", "true");
    }

    @Test
    public void testInfoLog() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger("ROOT");
        AsyncAppender asyncAppender = (AsyncAppender) rootLogger.getAppender("LogDna");
        LogDnaAppender appender = (LogDnaAppender) asyncAppender.getAppender("LogDnaHttp");

        assertNotNull(appender);

        assertNotNull(appender.ingestUrl);
        assertTrue(appender.headers.containsKey("apikey"));
        assertEquals("LogDnaTest", appender.appName);

        assertEquals(2, appender.mdcFields.size());
        assertEquals("requestId", appender.mdcFields.get(0));
        assertEquals("requestTime", appender.mdcFields.get(1));

        assertEquals(2, appender.mdcTypes.size());
        assertEquals("string", appender.mdcTypes.get(0));
        assertEquals("int", appender.mdcTypes.get(1));

        assertEquals("dev", appender.tags);
        assertEquals(5000, appender.connectTimeout);
        assertEquals(10000, appender.readTimeout);

        rootLogger.info("I am Groot");
    }

}