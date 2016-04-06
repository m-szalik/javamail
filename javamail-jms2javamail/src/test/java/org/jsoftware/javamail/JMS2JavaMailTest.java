package org.jsoftware.javamail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JMS2JavaMailTest {
    private MimeMessage source;
    private JavaMailSessionDelegate sessionDelegate;
    private JavaMailJMSStatistics javaMailJMSStatistics;
    private JMS2JavaMail jms2JavaMail;

    @Before
    public void setUp() throws Exception {
        final Session session = Session.getDefaultInstance(new Properties());
        source = new MimeMessage(session);
        sessionDelegate = Mockito.mock(JavaMailSessionDelegate.class);
        javaMailJMSStatistics = Mockito.mock(JavaMailJMSStatistics.class);
        jms2JavaMail = new JMS2JavaMail(sessionDelegate, javaMailJMSStatistics);
        when(sessionDelegate.createMimeMessage(any(InputStream.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
            return new MimeMessage(session, (InputStream) invocation.getArguments()[0]);
            }
        });
    }


    private static BytesMessage bytesMessageFor(MimeMessage mimeMessage, Address[] addresses) throws JMSException, IOException, MessagingException {
        mimeMessage.setContent("text", "plain/text");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeUTF("testProto");
        oos.writeObject(addresses == null ? new Address[0] : addresses);
        mimeMessage.writeTo(oos);
        BytesMessage message = Mockito.mock(BytesMessage.class);
        final ByteArrayInputStream messageBytes = new ByteArrayInputStream(out.toByteArray());
        when(message.readBytes(any(byte[].class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
            return messageBytes.read((byte[]) invocation.getArguments()[0]);
            }
        });
        return message;
    }


    @Test
    public void testSuccessWithConnectedTransport() throws Exception {
        source.setSubject("msg subject");
        source.setFrom("from@server.nowhere");
        source.setHeader("Header1", "Value1");
        BytesMessage message = bytesMessageFor(source, new Address[]{new InternetAddress("to@server.nowhere")});
        Transport transport = Mockito.mock(Transport.class);
        when(transport.isConnected()).thenReturn(true);
        when(sessionDelegate.findTransport(eq("testProto"))).thenReturn(transport);
        jms2JavaMail.onMessage(message);
        verify(message, times(1)).acknowledge();
        verify(transport, never()).connect();
        verify(transport, never()).close();
        ArgumentCaptor<MimeMessage> mimeMessageArgumentCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        ArgumentCaptor<Address[]> addressArgumentCaptor = ArgumentCaptor.forClass(Address[].class);
        verify(transport, times(1)).sendMessage(mimeMessageArgumentCaptor.capture(), addressArgumentCaptor.capture());
        MimeMessage dst = mimeMessageArgumentCaptor.getValue();
        InternetAddress toAddr = (InternetAddress) addressArgumentCaptor.getValue()[0];
        Assert.assertEquals("to@server.nowhere", toAddr.getAddress());
        Assert.assertEquals(source.getSubject(), dst.getSubject());
        Assert.assertArrayEquals(source.getFrom(), dst.getFrom());
        Assert.assertArrayEquals(source.getHeader("Header1"), dst.getHeader("Header1"));
        verify(javaMailJMSStatistics, times(1)).onSuccess(any(MimeMessage.class), any(Address[].class));
        verify(javaMailJMSStatistics, never()).onFailure(any(MimeMessage.class), any(Address[].class), any(Exception.class));
    }


    @Test
    public void testSuccessWithDisconnectedTransport() throws Exception {
        BytesMessage message = bytesMessageFor(source, new Address[]{new InternetAddress("to@server.nowhere")});
        Transport transport = Mockito.mock(Transport.class);
        when(transport.isConnected()).thenReturn(false);
        when(sessionDelegate.findTransport(eq("testProto"))).thenReturn(transport);
        jms2JavaMail.onMessage(message);
        verify(message, times(1)).acknowledge();
        verify(transport, times(1)).connect();
        verify(transport, times(1)).close();
        verify(transport, times(1)).sendMessage(any(MimeMessage.class), any(Address[].class));
    }


    @Test
    public void testFailNoTransportForProtocol() throws Exception {
        BytesMessage message = bytesMessageFor(source, new Address[]{new InternetAddress("to@server.nowhere")});
        Exception exception = new NoSuchProviderException();
        when(sessionDelegate.findTransport(eq("testProto"))).thenThrow(exception);
        jms2JavaMail.onMessage(message);
        verify(message, never()).acknowledge();
        ArgumentCaptor<Exception> exceptionArgumentCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(javaMailJMSStatistics, never()).onSuccess(any(MimeMessage.class), any(Address[].class));
        verify(javaMailJMSStatistics, times(1)).onFailure(any(MimeMessage.class), any(Address[].class), exceptionArgumentCaptor.capture());
        Assert.assertEquals(exception, exceptionArgumentCaptor.getValue());
    }


    @Test
    public void testNotBytesJMSMessage() throws Exception {
        TextMessage textMessage = Mockito.mock(TextMessage.class);
        jms2JavaMail.onMessage(textMessage);
        verify(javaMailJMSStatistics, never()).onSuccess(any(MimeMessage.class), any(Address[].class));
        verify(javaMailJMSStatistics, never()).onFailure(any(MimeMessage.class), any(Address[].class), any(Exception.class));
        verify(textMessage, times(1)).acknowledge();
    }


    @Test(expected = IllegalStateException.class)
    public void testCheckForNullSession() throws Exception {
        JMS2JavaMail jms2JavaMail = new JMS2JavaMail();
        jms2JavaMail.init();
    }
}