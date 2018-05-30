package net.zileo.logback.logdna;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * @author jlannoy
 */
public class LogDnaAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private final static String CUSTOM_USER_AGENT = "LogDna Logback Appender";

    private final Logger errorLog = LoggerFactory.getLogger(LogDnaAppender.class);

    private final MultivaluedMap<String, Object> headers;

    private final Client client;

    private final ObjectMapper mapper;

    private final String hostname;

    // Assignable fields

    PatternLayoutEncoder encoder;

    String appName;

    String ingestUrl = "https://logs.logdna.com/logs/ingest";

    List<String> mdcFields = new ArrayList<String>();

    List<String> mdcTypes = new ArrayList<String>();

    String tags;

    /**
     * Appender initialization.
     */
    public LogDnaAppender() {
        this.hostname = identifyHostname();

        this.headers = new MultivaluedHashMap<String, Object>();
        this.headers.add("User-Agent", CUSTOM_USER_AGENT);
        this.headers.add("Accept", MediaType.APPLICATION_JSON);
        this.headers.add("Content-Type", MediaType.APPLICATION_JSON);

        this.client = ClientBuilder.newClient();

        this.mapper = new ObjectMapper();
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
    }

    private String identifyHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    @Override
    protected void append(ILoggingEvent event) {

        if (event.getLoggerName().equals(LogDnaAppender.class.getName())) {
            return;
        }

        try {
            String jsonData = this.mapper.writeValueAsString(buildPostData(event));

            Response response = client.target(ingestUrl) //
                    .queryParam("hostname", this.hostname) //
                    .queryParam("now", System.currentTimeMillis()) //
                    .queryParam("tags", tags) //
                    .request().headers(headers).post(Entity.json(jsonData));

            if (response.getStatus() != 200) {
                errorLog.error("Error calling LogDna : {} ({})", response.readEntity(String.class), response.getStatus());
            }

        } catch (JsonProcessingException e) {
            errorLog.error("Error processing JSON data : " + e.getMessage(), e);
        } catch (Exception e) {
            errorLog.error("Error calling LogDna : " + e.getMessage(), e);
        }

    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> buildPostData(ILoggingEvent event) {
        Map<String, Object> line = new HashMap<String, Object>();
        line.put("timestamp", event.getTimeStamp());
        line.put("level", event.getLevel().toString());
        line.put("app", this.appName);
        line.put("line", this.encoder != null ? new String(this.encoder.encode(event)) : event.getFormattedMessage());

        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("logger", event.getLoggerName());
        if (mdcFields.size() > 0 && !event.getMDCPropertyMap().isEmpty()) {
            for (Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
                if (mdcFields.contains(entry.getKey())) {
                    String type = mdcTypes.get(mdcFields.indexOf(entry.getKey()));
                    meta.put(entry.getKey(), getMetaValue(type, entry.getValue()));
                }
            }
        }
        line.put("meta", meta);

        Map<String, Object> lines = new HashMap<String, Object>();
        lines.put("lines", Arrays.asList(line));
        return lines;
    }

    private Object getMetaValue(String type, String value) {
        try {
            if ("int".equals(type)) {
                return Integer.valueOf(value);
            }
            if ("long".equals(type)) {
                return Long.valueOf(value);
            }
            if ("boolean".equals(type)) {
                return Boolean.valueOf(value);
            }
        } catch (NumberFormatException e) {

        }
        return value;

    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Sets the application name for LogDNA indexation.
     * 
     * @param appName
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * Sets the LogDNA ingest API url.
     * 
     * @param ingestUrl
     */
    public void setIngestUrl(String ingestUrl) {
        this.ingestUrl = ingestUrl;
    }

    /**
     * Sets your LogDNA ingest API key.
     * 
     * @param apiKey
     */
    public void setIngestKey(String apiKey) {
        this.headers.add("apikey", apiKey);
    }

    /**
     * Sets the MDC fields that needs to be sent inside LogDNA metadata, separated by a comma.
     * 
     * @param mdcFields
     */
    public void setMdcFields(String mdcFields) {
        this.mdcFields = Arrays.asList(mdcFields.split(","));
    }

    /**
     * Sets the MDC fields types that will be sent inside LogDNA metadata, in the same order as <i>mdcFields</i> are set
     * up, separated by a comma.
     * 
     * @param mdcTypes
     */
    public void setMdcTypes(String mdcTypes) {
        this.mdcTypes = Arrays.asList(mdcTypes.split(","));
    }

    /**
     * Sets the tags that needs to be sent to LogDNA, for grouping hosts for example.
     * 
     * @param tags
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

}