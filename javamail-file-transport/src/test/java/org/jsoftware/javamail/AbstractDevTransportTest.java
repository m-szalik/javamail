package org.jsoftware.javamail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * @author szalik
 */
public class AbstractDevTransportTest {
    private MimeMessage message;

    @Before
    public void setup() throws MessagingException, IOException {
        Properties properties = new Properties();
        properties.put("mail.files.path", "target" + File.separatorChar + "output");
        Session session = Session.getDefaultInstance(properties);
        message = new MimeMessage(session);
        message.setFrom("Test <test@jsoftware.org>");
    }

    @Test(expected = MessagingException.class)
    public void testValidateAndPrepareEmptyNoAdd() throws Exception {
        AbstractDevTransport.validateAndPrepare(message, new Address[0]);
    }

    @Test
    public void testValidateAndPrepareEmptyAdd() throws Exception {
        AbstractDevTransport.validateAndPrepare(message, new Address[] {new InternetAddress("abc@test.com")});
        Assert.assertEquals(1, message.getRecipients(Message.RecipientType.TO).length);
    }

    @Test
    public void testValidateAndPrepareDuplicate() throws Exception {
        InternetAddress internetAddress = new InternetAddress("abc@test.com");
        message.addRecipient(Message.RecipientType.CC, internetAddress);
        AbstractDevTransport.validateAndPrepare(message, new Address[] {internetAddress, new InternetAddress("abc@jsoftware.org") });
        Assert.assertEquals(1, message.getRecipients(Message.RecipientType.TO).length);
        Assert.assertEquals(1, message.getRecipients(Message.RecipientType.CC).length);
    }
}
