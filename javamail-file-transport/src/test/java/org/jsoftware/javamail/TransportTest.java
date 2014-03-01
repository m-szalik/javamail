package org.jsoftware.javamail;

import org.junit.Before;
import org.junit.Test;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

//@Ignore
public class TransportTest {
    private Session session;

    @Before
    public void setup() {
        Properties properties = new Properties();
        properties.put("mail.files.path", "target/output");
        session = Session.getDefaultInstance(properties);
    }

	@Test
	public void transportTxtTest() throws MessagingException {
		Transport transport = session.getTransport("filetxt");
		MimeMessage msg = generateMessage();
        transport.sendMessage(msg, new Address[] { new InternetAddress("abc@test.com") });
	}

    @Test
    public void transportMsgTest() throws MessagingException {
        Transport transport = session.getTransport("filemsg");
        MimeMessage msg = generateMessage();
        transport.sendMessage(msg, new Address[] { new InternetAddress("abc@test.com") });
    }

    @Test
    public void transportNOPTest() throws MessagingException {
        Transport transport = session.getTransport("nop");
        MimeMessage msg = generateMessage();
        transport.sendMessage(msg, new Address[] { new InternetAddress("abc@test.com") });
    }


    private MimeMessage generateMessage() throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom("Test <from@test.com>");
        msg.setText("Text");
        msg.setSubject("subject");
        return msg;
    }


}

