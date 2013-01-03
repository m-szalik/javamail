package org.jsoftware.javamail;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TransportTest {

	@Test
	public void transportTest() throws MessagingException {
		Properties properties = new Properties();
		properties.put("mail.smtpfile.path", "target/output");
		Session session = Session.getDefaultInstance(properties);
		Transport transport = session.getTransport("smtpfile");
		MimeMessage msg = new MimeMessage(session);
		msg.setText("Text");
		msg.setSubject("subject");
		transport.sendMessage(msg, new Address[] { new InternetAddress("abc@test.com") });
	}

}

