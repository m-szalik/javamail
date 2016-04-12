package org.jsoftware.javamail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.ConnectionListener;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author szalik
 */
public class AbstractTransportTest {
    private MimeMessage message;
    private AbstractTransport transport;
    private ConnectionListener connectionListener;
    private TransportListener transportListener;

    @Before
    public void setUp() throws MessagingException, IOException {
        Properties properties = new Properties();
        properties.put("mail.files.path", "target" + File.separatorChar + "output");
        Session session = Session.getDefaultInstance(properties);
        message = new MimeMessage(session);
        message.setFrom("Test <test@jsoftware.org>");
        connectionListener = Mockito.mock(ConnectionListener.class);
        transportListener = Mockito.mock(TransportListener.class);
        transport = new AbstractTransport(session, new URLName("AbstractDev")) {
            @Override
            public void sendMessage(Message message, Address[] addresses) throws MessagingException {
                validateAndPrepare(message, addresses);
            }
        };
        transport.addConnectionListener(connectionListener);
        transport.addTransportListener(transportListener);
    }

    @Test(expected = MessagingException.class)
    public void testValidateAndPrepareEmptyNoAdd() throws Exception {
        transport.validateAndPrepare(message, new Address[0]);
    }

    @Test
    public void testValidateAndPrepareEmptyAdd() throws Exception {
        transport.validateAndPrepare(message, new Address[] {new InternetAddress("abc@test.com")});
        Assert.assertEquals(1, message.getRecipients(Message.RecipientType.TO).length);
    }

    @Test
    public void testValidateAndPrepareDuplicate() throws Exception {
        InternetAddress internetAddress = new InternetAddress("abc@test.com");
        message.addRecipient(Message.RecipientType.CC, internetAddress);
        transport.validateAndPrepare(message, new Address[] {internetAddress, new InternetAddress("abc@jsoftware.org") });
        Assert.assertEquals(1, message.getRecipients(Message.RecipientType.TO).length);
        Assert.assertEquals(1, message.getRecipients(Message.RecipientType.CC).length);
    }

    @Test
    public void testExtractParts() throws Exception {
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart part;
        part = new MimeBodyPart();
        part.setContent("Text 1", "text/plain");
        multipart.addBodyPart(part);
        part = new MimeBodyPart();
        part.setContent("Text 2", "text/plain");
        multipart.addBodyPart(part);
        MimeMultipart multipartChild = new MimeMultipart();
        part = new MimeBodyPart();
        part.setContent("Text 3", "text/plain");
        multipartChild.addBodyPart(part);
        part = new MimeBodyPart();
        part.setContent(multipartChild, "alternative");
        multipart.addBodyPart(part);
        Map<String,Collection<String>> map = AbstractTransport.extractTextParts(multipart);
        Assert.assertEquals(1, map.size());
        Collection<String> content = map.get("text/plain");
        Assert.assertNotNull(map);
        Assert.assertArrayEquals(new String[] {"Text 1", "Text 2", "Text 3"}, content.toArray());
    }

    @Test
    public void testConnectClose() throws Exception {
        transport.connect();
        Thread.sleep(200);
        verify(connectionListener, times(1)).opened(any(ConnectionEvent.class));
        transport.close();
        Thread.sleep(200);
        verify(connectionListener).disconnected(any(ConnectionEvent.class));
        verify(connectionListener).closed(any(ConnectionEvent.class));
    }

    @Test(expected = MessagingException.class)
    public void testNoFromNull() throws Exception {
        try {
            message.setFrom((String) null);
            transport.validateAndPrepare(message, new Address[]{});
        } finally {
            Thread.sleep(200);
            verify(transportListener).messageNotDelivered(any(TransportEvent.class));
        }
    }

}
