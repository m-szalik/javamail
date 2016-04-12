package org.jsoftware.javamail;

/**
 * Exception while Transport is created.
 * @author szalik
 */
public class TransportCreationException extends RuntimeException {

    public TransportCreationException(String msg, Exception initCause) {
        super(msg, initCause);
    }
}
