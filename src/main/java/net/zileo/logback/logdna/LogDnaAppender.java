package net.zileo.logback.logdna;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * Logback appender for sending logs to <a href="https://logdna.com">LogDNA.com</a>.
 * 
 * @author jlannoy
 */
public class LogDnaAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final String CUSTOM_USER_AGENT = "LogDna Logback Appender";

    private final Logger errorLog = LoggerFactory.getLogger(LogDnaAppender.class);

    private final ObjectMapper dataMapper;

    private final ObjectMapper responseMapper;

    private Client client;

    private boolean disabled;

    protected final MultivaluedMap<String, Object> headers;

    // Assignable fields

    protected String hostname;

    protected PatternLayoutEncoder encoder;

    protected String appName;

    protected String ingestUrl = "https://logs.logdna.com/logs/ingest";

    protected List<String> mdcFields = new ArrayList<>();

    protected List<String> mdcTypes = new ArrayList<>();

    protected String tags;

    protected long connectTimeout = 0;

    protected long readTimeout = 0;

    protected boolean useTimeDrift = true;

    /**
     * Appender initialization.
     */
    public LogDnaAppender() {
        this.headers = new MultivaluedHashMap<>();
        this.headers.add("User-Agent", CUSTOM_USER_AGENT);
        this.headers.add("Accept", MediaType.APPLICATION_JSON);
        this.headers.add("Content-Type", MediaType.APPLICATION_JSON);

        this.dataMapper = new ObjectMapper();
        this.dataMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.dataMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);

        this.responseMapper = new ObjectMapper();
        this.responseMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String identifyHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    // Postpone client initialization to allow timeouts configuration
    protected Client client() {
        if (this.client == null) {

            if (this.hostname == null) {
                this.hostname = identifyHostname();
            }

            this.client = ClientBuilder.newBuilder() //
                    .connectTimeout(this.connectTimeout, TimeUnit.MILLISECONDS) //
                    .readTimeout(this.readTimeout, TimeUnit.MILLISECONDS) //
                    .build();
        }

        return this.client;
    }

    /**
     * @see ch.qos.logback.core.UnsynchronizedAppenderBase#append(java.lang.Object)
     */
    @Override
    protected void append(ILoggingEvent event) {

        if (disabled) {
            return;
        }

        if (event.getLoggerName().equals(LogDnaAppender.class.getName())) {
            return;
        }

        if (!this.headers.containsKey("apikey") || this.headers.getFirst("apikey").toString().isBlank()) {
            errorLog.warn("Empty ingest API key for LogDNA ; disabling LogDnaAppender");
            this.disabled = true;
            return;
        }

        try {
            String jsonData = convertLogEventToJson(event);

            Response response = callIngestApi(jsonData);

            if (response.getStatus() != 200) {
                LogDnaResponse logDnaResponse = convertResponseToObject(response);
                errorLog.error("Error calling LogDna : {} ({})", logDnaResponse.getError(), response.getStatus());
            }

        } catch (JsonProcessingException e) {
            errorLog.error("Error processing JSON data : {}", e.getMessage());

        } catch (Exception e) {
            errorLog.error("Error calling LogDna : {}", e.getMessage());
        }

    }

    protected String convertLogEventToJson(ILoggingEvent event) throws JsonProcessingException {
        return this.dataMapper.writeValueAsString(buildPostData(event));
    }

    protected LogDnaResponse convertResponseToObject(Response response) throws JsonProcessingException {
        return this.responseMapper.readValue(response.readEntity(String.class), LogDnaResponse.class);
    }

    /**
     * Call LogDna API posting given JSON formated string.
     * 
     * @param jsonData
     *            a json oriented map
     * @return the http response
     */

    protected Response callIngestApi(String jsonData) {
        WebTarget wt = client().target(ingestUrl) //
                .queryParam("hostname", this.hostname) //
                .queryParam("tags", tags);

        if (useTimeDrift) {
            wt = wt.queryParam("now", System.currentTimeMillis());
        }

        return wt.request() //
                .headers(headers) //
                .post(Entity.json(jsonData));
    }

    /**
     * Converts a logback logging event to a JSON oriented map.
     * 
     * @param event
     *            the logging event
     * @return a json oriented map
     */
    protected Map<String, Object> buildPostData(ILoggingEvent event) {
        Map<String, Object> line = new HashMap<>();
        line.put("timestamp", event.getTimeStamp());
        line.put("level", event.getLevel().toString());
        line.put("app", this.appName);
        line.put("line", this.encoder != null ? new String(this.encoder.encode(event)) : event.getFormattedMessage());

        Map<String, Object> meta = new HashMap<>();
        meta.put("logger", event.getLoggerName());
        if (!mdcFields.isEmpty() && !event.getMDCPropertyMap().isEmpty()) {
            for (Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
                if (mdcFields.contains(entry.getKey())) {
                    String type = mdcTypes.get(mdcFields.indexOf(entry.getKey()));
                    meta.put(entry.getKey(), getMetaValue(type, entry.getValue()));
                }
            }
        }
        line.put("meta", meta);

        Map<String, Object> lines = new HashMap<>();
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
            errorLog.warn("Error getting meta value : {}", e.getMessage());
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
     *            application name
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * Sets the LogDNA ingest API url.
     * 
     * @param ingestUrl
     *            logdna url
     */
    public void setIngestUrl(String ingestUrl) {
        this.ingestUrl = ingestUrl;
    }

    /**
     * Sets your LogDNA ingest API key.
     * 
     * @param ingestKey
     *            your ingest key
     */
    public void setIngestKey(String ingestKey) {
        this.headers.add("apikey", ingestKey);
    }

    /**
     * Sets the MDC fields that needs to be sent inside LogDNA metadata, separated by a comma.
     * 
     * @param mdcFields
     *            MDC fields to use
     */
    public void setMdcFields(String mdcFields) {
        this.mdcFields = Arrays.asList(mdcFields.split(","));
    }

    /**
     * Sets the MDC fields types that will be sent inside LogDNA metadata, in the same order as <i>mdcFields</i> are set
     * up, separated by a comma. Possible values are <i>string</i>, <i>boolean</i>, <i>int</i> and <i>long</i>. The last
     * two will result as an indexed <i>number</i> in LogDNA's console.
     * 
     * @param mdcTypes
     *            MDC fields types
     */
    public void setMdcTypes(String mdcTypes) {
        this.mdcTypes = Arrays.asList(mdcTypes.split(","));
    }

    /**
     * Sets the tags that needs to be sent to LogDNA, for grouping hosts for example.
     * 
     * @param tags
     *            fixed tags
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * Set whether using time drift. If set true, now parameter is supplied (https://docs.logdna.com/reference).
     *
     * @param useTimeDrift
     *            true: Use time drift. false: Do not use time drift.
     */
    public void setUseTimeDrift(String useTimeDrift) {
        this.useTimeDrift = !useTimeDrift.equalsIgnoreCase("false");
    }

    /**
     * Force a given value for the hostname LogDNA parameter.
     *
     * @param hostname
     *            local hostname value
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets the connection timeout of the underlying HTTP client, in milliseconds.
     * 
     * @param connectTimeout
     *            client connection timeout
     */
    public void setConnectTimeout(Long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Sets the read timeout of the underlying HTTP client, in milliseconds.
     * 
     * @param readTimeout
     *            client read timeout
     */
    public void setReadTimeout(Long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isDisabled() {
        return this.disabled;
    }
}
