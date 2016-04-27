package org.jsoftware.javamail;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.mail.Address;
import javax.mail.Message;
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
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.jsoftware.javamail.AbstractTransportTest.waitForListeners;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
    public void setUp() throws AddressException, IOException {
        String outDirName = "target/output";
        Properties properties = new Properties();
        outDir = new File(outDirName);
        if (outDir.exists()) {
            File[] files = outDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (! f.delete()) {
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Unable to delete test file " + f.getAbsolutePath());
                    }
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
        String fileContent = new String(outputStream.toByteArray());
        assertEquals(fileContent, IOUtils.toString(getClass().getResourceAsStream("transportTest-expected.txt"), "UTF-8"));
	}

    @Test
    public void transportMsgTest() throws MessagingException, IOException, NoSuchAlgorithmException {
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filemsg");
        transport.writeMessage(generateMessage(), outputStream);
        String fileContent = new String(outputStream.toByteArray());
        assertEquals(normalizeContent(fileContent), normalizeContent(IOUtils.toString(getClass().getResourceAsStream("transportTest-expected.msg"), "UTF-8")));
    }

    private static String normalizeContent(String input) {
        Pattern trimTailPattern = Pattern.compile("[ \t]+$");
        StringBuilder sb = new StringBuilder();
        for(String line : input.split("\n")) {
            line = trimTailPattern.matcher(line).replaceAll("");
            if (line.startsWith("Message-ID:")) {
                sb.append("Message-ID: testID\n");
                continue;
            }
            if (line.startsWith("\tboundary=")) {
                sb.append("\tboundary=\"tb\"\n");
                continue;
            }
            if (line.startsWith("------=_Part")) {
                sb.append("------=_Part_XYZ--\n");
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString().replaceAll("\r", "");
    }

    @Test
    public void transportNOPTest() throws Exception {
        TransportListener transportListener = Mockito.mock(TransportListener.class);
        Message message = generateMessage();
        Transport transport = session.getTransport("nop");
        transport.addTransportListener(transportListener);
        transport.sendMessage(message, toAddress);
        waitForListeners();
        ArgumentCaptor<TransportEvent> transportEventArgumentCaptor = ArgumentCaptor.forClass(TransportEvent.class);
        verify(transportListener).messageDelivered(transportEventArgumentCaptor.capture());
        TransportEvent event = transportEventArgumentCaptor.getValue();
        assertEquals(message, event.getMessage());
        assertEquals(TransportEvent.MESSAGE_DELIVERED, event.getType());
        assertArrayEquals(toAddress, event.getValidSentAddresses());
    }

    @Test
    public void transportSendMessageTest() throws Exception {
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filetxt");
        MimeMessage message = generateMessage();
        transport.sendMessage(message, toAddress);
        File[] msgFiles = outDir.listFiles();
        assertNotNull(msgFiles);
        assertEquals(1, msgFiles.length);
        assertTrue(msgFiles[0].length() > 0);
        assertTrue(msgFiles[0].getName().endsWith(".txt"));
    }

    @Test
    public void testListenerOnSuccess() throws Exception {
        Address[] to = new Address[] {new InternetAddress("to@server.nowhere")};
        Message message = generateMessage();
        TransportListener transportListener = Mockito.mock(TransportListener.class);
        AbstractFileTransport transport = (AbstractFileTransport) session.getTransport("filemsg");
        transport.addTransportListener(transportListener);
        transport.sendMessage(message, to);
        waitForListeners();
        ArgumentCaptor<TransportEvent> transportEventArgumentCaptor = ArgumentCaptor.forClass(TransportEvent.class);
        verify(transportListener).messageDelivered(transportEventArgumentCaptor.capture());
        TransportEvent event = transportEventArgumentCaptor.getValue();
        assertEquals(message, event.getMessage());
        assertEquals(TransportEvent.MESSAGE_DELIVERED, event.getType());
        assertArrayEquals(to, event.getValidSentAddresses());
        verify(transportListener).messageDelivered(any(TransportEvent.class));
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
            waitForListeners();
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

}
