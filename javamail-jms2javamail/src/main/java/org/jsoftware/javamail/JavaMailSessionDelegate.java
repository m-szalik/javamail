package org.jsoftware.javamail;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;

/**
 * Delegate Session operations.
 * @author szalik
 */
public interface JavaMailSessionDelegate {

    Transport findTransport(String protocolToUse) throws NoSuchProviderException;

    MimeMessage createMimeMessage(InputStream inputStream) throws MessagingException;

}
