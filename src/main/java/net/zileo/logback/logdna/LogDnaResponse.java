package net.zileo.logback.logdna;

/**
 * Holder for LogDna error message.
 * 
 * @author jlannoy
 */
public class LogDnaResponse {

    private String error, code, status;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
