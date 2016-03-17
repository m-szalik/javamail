package org.jsoftware.javamail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransportTest {
    private Address[] toAddress;
    private Session session;
    private ByteArrayOutputStream outputStream;
    private File outDir;

    @Before
    public void setup() throws AddressException, IOException {
        String outDirName = "target/output";
        Properties properties = new Properties();
        outDir = new File(outDirName);
        if (outDir.exists()) {
            File[] files = outDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
        properties.put("mail.files.path", outDirName);
        session = Session.getDefaultInstance(properties);
        toAddress = new Address[] { new InternetAddress("abc@test.com") };
        outputStream = new ByteArrayOutputStream();
    }



	@Test
	public void transportTxtTest() throws MessagingException, IOException, NoSuchAlgorithmException {
		AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filetxt");
        transport.writeMessage(generateMessage(), outputStream);
        org.junit.Assert.assertEquals("10e5c47d151c47ef62c23ca998d1b55a", md5sumFromOutput());
	}

    @Test
    public void transportMsgTest() throws MessagingException, IOException, NoSuchAlgorithmException {
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filemsg");
        transport.writeMessage(generateMessage(), outputStream);
        org.junit.Assert.assertEquals("f351530889938d0625739a37bab0b992", md5sumFromOutput());
    }

    @Test
    public void transportNOPTest() throws MessagingException {
        Transport transport = session.getTransport("nop");
        transport.sendMessage(generateMessage(), toAddress);
    }

    @Test
    public void transportSendMessageTest() throws Exception {
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filetxt");
        MimeMessage message = generateMessage();
        transport.sendMessage(message, toAddress);
        File[] msgFiles = outDir.listFiles();
        Assert.assertNotNull(msgFiles);
        Assert.assertEquals(1, msgFiles.length);
        Assert.assertTrue(msgFiles[0].length() > 0);
        Assert.assertTrue(msgFiles[0].getName().endsWith(".txt"));
    }

    @Test
    public void testListenerOnSuccess() throws Exception {
        TransportListener transportListener = Mockito.mock(TransportListener.class);
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filemsg");
        transport.addTransportListener(transportListener);
        transport.sendMessage(generateMessage(), new Address[] {new InternetAddress("to@server.nowhere")});
        Thread.sleep(200);
        verify(transportListener, times(1)).messageDelivered(any(TransportEvent.class));
    }

    @Test
    public void testListenerOnError() throws Exception {
        TransportListener transportListener = Mockito.mock(TransportListener.class);
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filemsg");
        transport.addTransportListener(transportListener);
        MimeMessage message = Mockito.mock(MimeMessage.class);
        when(message.getFrom()).thenReturn(new Address[]{new InternetAddress("from@server.nowhere")});
        doThrow(new IOException()).when(message).writeTo(any(OutputStream.class));
        try {
            transport.sendMessage(message, new Address[]{new InternetAddress("to@server.nowhere")});
            Assert.fail("Exception expected");
        } catch (MessagingException ex) {
            Thread.sleep(200);
            verify(transportListener, times(1)).messageNotDelivered(any(TransportEvent.class));
        }
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
        msg.addHeader("X-Custom-Header", "CustomValue");
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

