package org.jsoftware.javamail;

import org.junit.Before;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
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
        org.junit.Assert.assertEquals("4c7fb32ade5f1e860d9f8234b1ba8d5a", md5sumFromOutput());
	}

    @Test
    public void transportMsgTest() throws MessagingException, IOException, NoSuchAlgorithmException {
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filemsg");
        transport.writeMessage(generateMessage(), toAddress, outputStream);
        org.junit.Assert.assertEquals("789f256c33750a89b804a6c667e85d35", md5sumFromOutput());
    }

    @Test
    public void transportNOPTest() throws MessagingException {
        Transport transport = session.getTransport("nop");
        transport.sendMessage(generateMessage(), toAddress);
    }


    /**
     * Generate multi part / alternative message
     */
    private MimeMessage generateMessage() throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom("Test <from@test.com>");
        msg.setSubject("subject");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Body's text (text)", "UTF-8");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<p>Body's text <strong>(html)</strong></p>", "text/html; charset=UTF-8");
        Multipart multiPart = new MimeMultipart("alternative");
        multiPart.addBodyPart(textPart); // first
        multiPart.addBodyPart(htmlPart); // second
        msg.setContent(multiPart);
        return msg;
    }


    private String md5sumFromOutput() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String content = new String(outputStream.toByteArray(), "UTF-8");
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        for(String l : content.split("\n")) {
            if (! l.startsWith("Date:") && ! l.startsWith("Message-ID:") && ! l.contains("Part")) {
                m.update(l.getBytes());
            }
        }
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
    }
}

