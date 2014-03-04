package org.jsoftware.javamail;

import org.junit.Before;
import org.junit.Test;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

//@Ignore
public class TransportTest {
    private Address[] toAddress;
    private Session session;
    private ByteArrayOutputStream outputStream;

    @Before
    public void setup() throws AddressException, IOException {
        Properties properties = new Properties();
        properties.put("mail.files.path", "target/output");
        session = Session.getDefaultInstance(properties);
        toAddress = new Address[] { new InternetAddress("abc@test.com") };
        outputStream = new ByteArrayOutputStream();
    }



	@Test
	public void transportTxtTest() throws MessagingException, IOException, NoSuchAlgorithmException {
		AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filetxt");
        transport.writeMessage(generateMessage(), toAddress, outputStream);
	}

    @Test
    public void transportMsgTest() throws MessagingException, IOException, NoSuchAlgorithmException {
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filemsg");
        transport.writeMessage(generateMessage(), toAddress, outputStream);
    }

    @Test
    public void transportNOPTest() throws MessagingException {
        Transport transport = session.getTransport("nop");
        transport.sendMessage(generateMessage(), toAddress);
    }


    private MimeMessage generateMessage() throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom("Test <from@test.com>");
        msg.setText("Text");
        msg.setSubject("subject");
        return msg;
    }

}

