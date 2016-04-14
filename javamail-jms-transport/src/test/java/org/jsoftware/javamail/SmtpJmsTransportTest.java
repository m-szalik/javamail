package org.jsoftware.javamail;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.InternetAddress;
import javax.naming.Context;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SmtpJmsTransportTest {
    private BytesMessage bytesMessage;
    private QueueSender queueSender;
    private SmtpJmsTransport transport;
    private TransportListener transportListener;

    @Before
    public void setUp() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestContextFactory.class.getName());
        QueueConnectionFactory queueConnectionFactory = Mockito.mock(QueueConnectionFactory.class);
        Queue queue = Mockito.mock(Queue.class);
        Context context = Mockito.mock(Context.class);
        TestContextFactory.context = context;
        when(context.lookup(eq("jms/queueConnectionFactory"))).thenReturn(queueConnectionFactory);
        when(context.lookup(eq("jms/mailQueue"))).thenReturn(queue);
        queueSender = Mockito.mock(QueueSender.class);
        QueueConnection queueConnection = Mockito.mock(QueueConnection.class);
        when(queueConnectionFactory.createQueueConnection()).thenReturn(queueConnection);
        when(queueConnectionFactory.createQueueConnection(anyString(), anyString())).thenReturn(queueConnection);
        QueueSession queueSession = Mockito.mock(QueueSession.class);
        bytesMessage = Mockito.mock(BytesMessage.class);
        when(queueSession.createBytesMessage()).thenReturn(bytesMessage);
        when(queueConnection.createQueueSession(anyBoolean(), anyInt())).thenReturn(queueSession);
        when(queueSession.createSender(eq(queue))).thenReturn(queueSender);
        transport = new SmtpJmsTransport(Session.getDefaultInstance(new Properties()), new URLName("jms"));
        transportListener = Mockito.mock(TransportListener.class);
        transport.addTransportListener(transportListener);
    }

    @Test
    public void testSendWithoutFrom() throws Exception {
        Message message = Mockito.mock(Message.class);
        Address[] sendTo = new Address[] { new InternetAddress("text@xtest.nowhere") };
        try {
            transport.sendMessage(message, sendTo);
            fail();
        } catch (MessagingException ex) {
            Thread.sleep(200);
            ArgumentCaptor<TransportEvent> transportEventArgumentCaptor = ArgumentCaptor.forClass(TransportEvent.class);
            verify(transportListener, times(1)).messageNotDelivered(transportEventArgumentCaptor.capture());
            TransportEvent event = transportEventArgumentCaptor.getValue();
            assertEquals(message, event.getMessage());
            assertEquals(TransportEvent.MESSAGE_NOT_DELIVERED, event.getType());
            assertArrayEquals(new Address[0], event.getInvalidAddresses());
            assertArrayEquals(new Address[0], event.getValidSentAddresses());
            assertArrayEquals(sendTo, event.getValidUnsentAddresses());
        }
    }

    @Test
    public void testSendWitEmptyFrom() throws Exception {
        Message message = Mockito.mock(Message.class);
        when(message.getFrom()).thenReturn(new Address[0]);
        try {
            transport.sendMessage(message, new Address[]{new InternetAddress("text@xtest.nowhere")});
            fail();
        } catch (MessagingException ex) {
            Thread.sleep(200);
            verify(transportListener, times(1)).messageNotDelivered(any(TransportEvent.class));
        }
    }


    @Test
    public void testDoNotCheckFormHeader() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("mail.smtpjms.validateFrom", "false");
        SmtpJmsTransport transport = new SmtpJmsTransport(Session.getInstance(properties), new URLName("jms"));
        Message message = Mockito.mock(Message.class);
        TransportListener listener = Mockito.mock(TransportListener.class);
        Address[] to = new Address[]{ new InternetAddress("text@xtest.nowhere") };
        transport.addTransportListener(listener);
        transport.sendMessage(message, to);
        Thread.sleep(200);
        ArgumentCaptor<TransportEvent> transportEventArgumentCaptor = ArgumentCaptor.forClass(TransportEvent.class);
        verify(listener).messageDelivered(transportEventArgumentCaptor.capture());
        TransportEvent event = transportEventArgumentCaptor.getValue();
        assertEquals(message, event.getMessage());
        assertEquals(TransportEvent.MESSAGE_DELIVERED, event.getType());
        assertArrayEquals(to, event.getValidSentAddresses());
    }


    @Test
    public void testNoAddresses() throws Exception {
        Message message = Mockito.mock(Message.class);
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.nowhere") });
        try {
            transport.sendMessage(message, new Address[]{}); // empty address list
            fail();
        } catch (MessagingException ex) {
            Thread.sleep(200);
            verify(transportListener).messageNotDelivered(any(TransportEvent.class));
        }
    }


    @Test
    public void testSend() throws Exception {
        final Address[] sendTo = new Address[] { new InternetAddress("text@xtest.nowhere") };
        Message message = Mockito.mock(Message.class);
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[]{"low"});
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.com") });
        when(message.getSubject()).thenReturn("msgSubject!");
        transport.sendMessage(message, sendTo);
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(bytesMessage, times(1)).writeBytes(bytesCaptor.capture());
        byte[] messageBuffer = bytesCaptor.getValue();
        assertNotNull(messageBuffer);
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(messageBuffer));
        String protocol = input.readUTF();
        Address[] inAddr = (Address[]) input.readObject();
        assertEquals(1, inAddr.length);
        assertArrayEquals(sendTo, inAddr);
        assertEquals("smtp", protocol);
        verify(message, times(1)).writeTo(any(ObjectOutputStream.class));
        Thread.sleep(200);
        ArgumentCaptor<TransportEvent> transportEventArgumentCaptor = ArgumentCaptor.forClass(TransportEvent.class);
        verify(transportListener, times(1)).messageDelivered(transportEventArgumentCaptor.capture());
        TransportEvent event = transportEventArgumentCaptor.getValue();
        assertEquals(message, event.getMessage());
        assertArrayEquals(new Address[0], event.getInvalidAddresses());
        assertArrayEquals(sendTo, event.getValidSentAddresses());
        assertArrayEquals(new Address[0], event.getValidUnsentAddresses());
    }

    @Test
    public void testSendNumberPriority() throws Exception {
        final int priority = 4;
        Message message = Mockito.mock(Message.class);
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[]{Integer.toString(priority)});
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.com") });
        transport.sendMessage(message, new Address[] { new InternetAddress("text@xtest.nowhere") });
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(bytesMessage, times(1)).setJMSPriority(captor.capture());
        Integer jmsPriority = captor.getValue();
        assertEquals(priority, jmsPriority.intValue());
    }

    @Test
    public void testSendInvalidPriority() throws Exception {
        SmtpJmsTransport transport = new SmtpJmsTransport(Session.getDefaultInstance(new Properties()), new URLName("jsm"));
        Message message = Mockito.mock(Message.class);
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[]{"invalid"});
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.com") });
        transport.sendMessage(message, new Address[] { new InternetAddress("text@xtest.nowhere") });
        verify(bytesMessage, never()).setJMSPriority(anyInt());
    }

    @Test
    public void testSendNumberPriorityXPriority() throws Exception {
        final int priority = 4;
        Message message = Mockito.mock(Message.class);
        when(message.getHeader(eq("X-Priority"))).thenReturn(new String[]{Integer.toString(priority)});
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.com") });
        transport.sendMessage(message, new Address[] { new InternetAddress("text@xtest.nowhere") });
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(bytesMessage, times(1)).setJMSPriority(captor.capture());
        Integer jmsPriority = captor.getValue();
        assertEquals(priority, jmsPriority.intValue());
    }

    @Test
    public void testSendInvalidPriorityXPriority() throws Exception {
        Message message = Mockito.mock(Message.class);
        when(message.getHeader(eq("X-Priority"))).thenReturn(new String[]{"invalid"});
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.com") });
        transport.sendMessage(message, new Address[] { new InternetAddress("text@xtest.nowhere") });
        verify(bytesMessage, never()).setJMSPriority(anyInt());
    }

    @Test
    public void testSendWithTTL() throws Exception {
        Message message = Mockito.mock(Message.class);
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_EXPIRE))).thenReturn(new String[]{"321"});
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.com") });
        transport.sendMessage(message, new Address[] { new InternetAddress("text@xtest.nowhere") });
        ArgumentCaptor<Long> ttlLongArgumentCaptor = ArgumentCaptor.forClass(Long.class);
        verify(queueSender, times(1)).setTimeToLive(ttlLongArgumentCaptor.capture());
        assertEquals(Long.valueOf(321), ttlLongArgumentCaptor.getValue());
    }

    @Test
    public void testFailOnJms() throws Exception {
        Message message = Mockito.mock(Message.class);
        Address[] to = new Address[] { new InternetAddress("text@xtest.nowhere") };
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.com") });
        doThrow(new JMSException("mock")).when(queueSender).send(any(javax.jms.Message.class));
        try {
            transport.sendMessage(message, to);
            fail();
        } catch (MessagingException ex) {
            Thread.sleep(200);
            ArgumentCaptor<TransportEvent> transportEventArgumentCaptor = ArgumentCaptor.forClass(TransportEvent.class);
            verify(transportListener).messageNotDelivered(transportEventArgumentCaptor.capture());
            TransportEvent event = transportEventArgumentCaptor.getValue();
            assertEquals(message, event.getMessage());
            assertEquals(TransportEvent.MESSAGE_NOT_DELIVERED, event.getType());
            assertArrayEquals(new Address[0], event.getValidSentAddresses());
            assertArrayEquals(to, event.getValidUnsentAddresses());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingTransportForDstProtocol() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("mail.smtpjms.dstProtocol", "notExistingOne");
        new SmtpJmsTransport(Session.getInstance(properties), new URLName("jms"));
    }

    @Test(expected = MessagingException.class)
    public void testErrorWritingMessage() throws Exception {
        Message message = Mockito.mock(Message.class);
        when(message.getFrom()).thenReturn(new Address[] { new InternetAddress("from@server.com") });
        when(message.getHeader(eq("Message-ID"))).thenReturn(new String[] {"mockMID"});
        doThrow(new IOException("mock")).when(message).writeTo(any(OutputStream.class));
        try {
            transport.sendMessage(message, new Address[]{new InternetAddress("text@xtest.nowhere")});
        } finally {
            Thread.sleep(200);
            verify(transportListener).messageNotDelivered(any(TransportEvent.class));
        }
    }

    @Test
    public void testJmsPriorityMapping() throws Exception {
        Message message = Mockito.mock(Message.class);
        // X-Send-priority
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[] {"low"});
        assertEquals(Integer.valueOf(1), SmtpJmsTransport.jmsPriority(message));
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[] {"high"});
        assertEquals(Integer.valueOf(8), SmtpJmsTransport.jmsPriority(message));
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[] {});
        assertNull(SmtpJmsTransport.jmsPriority(message));
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[] {"normal"});
        assertNull(SmtpJmsTransport.jmsPriority(message));
        // numeric value
        for(int p=0; p<10; p++) {
            when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[]{Integer.toString(p)});
            assertEquals(Integer.valueOf(p), SmtpJmsTransport.jmsPriority(message));
        }
        // less or over limit
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[]{"-2"});
        assertEquals(Integer.valueOf(0), SmtpJmsTransport.jmsPriority(message));
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[]{"12"});
        assertEquals(Integer.valueOf(9), SmtpJmsTransport.jmsPriority(message));
        // X-Priority
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[] {});
        when(message.getHeader(eq(SmtpJmsTransport.X_PRIORITY))).thenReturn(new String[]{"1"});
        assertEquals(Integer.valueOf(8), SmtpJmsTransport.jmsPriority(message));
        when(message.getHeader(eq(SmtpJmsTransport.X_PRIORITY))).thenReturn(new String[]{"2"});
        assertEquals(Integer.valueOf(6), SmtpJmsTransport.jmsPriority(message));
        when(message.getHeader(eq(SmtpJmsTransport.X_PRIORITY))).thenReturn(new String[]{"4"});
        assertEquals(Integer.valueOf(4), SmtpJmsTransport.jmsPriority(message));
        when(message.getHeader(eq(SmtpJmsTransport.X_PRIORITY))).thenReturn(new String[]{"5"});
        assertEquals(Integer.valueOf(1), SmtpJmsTransport.jmsPriority(message));
        when(message.getHeader(eq(SmtpJmsTransport.X_PRIORITY))).thenReturn(new String[]{"10"}); // unmapped
        assertNull(SmtpJmsTransport.jmsPriority(message));
        // X-Send-priority over X-Send-priority header
        when(message.getHeader(eq(SmtpJmsTransport.X_SEND_PRIORITY))).thenReturn(new String[] {"high"});
        when(message.getHeader(eq(SmtpJmsTransport.X_PRIORITY))).thenReturn(new String[]{"5"});
        assertEquals(Integer.valueOf(8), SmtpJmsTransport.jmsPriority(message));
    }
}