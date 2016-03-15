package org.jsoftware.javamail;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.jms.BytesMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test between javamail-jms-transport and javamail-jms2javamail.
 * @author szalik
 */
public class JmsIntegrationTest {

    @Test
    public void testJmsIntegration() throws Exception {
        final Session javamail = Session.getDefaultInstance(new Properties());
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, IntegrationTestContextFactory.class.getName());
        QueueConnectionFactory queueConnectionFactory = Mockito.mock(QueueConnectionFactory.class);
        Queue queue = Mockito.mock(Queue.class);
        Context context = Mockito.mock(Context.class);
        IntegrationTestContextFactory.context = context;
        when(context.lookup(eq("jms/queueConnectionFactory"))).thenReturn(queueConnectionFactory);
        when(context.lookup(eq("jms/mailQueue"))).thenReturn(queue);
        QueueSender queueSender = Mockito.mock(QueueSender.class);
        QueueConnection queueConnection = Mockito.mock(QueueConnection.class);
        // convert mimeMessage to JMS message
        when(queueConnectionFactory.createQueueConnection()).thenReturn(queueConnection);
        when(queueConnectionFactory.createQueueConnection(anyString(), anyString())).thenReturn(queueConnection);
        QueueSession queueSession = Mockito.mock(QueueSession.class);
        BytesMessage bytesMessage = Mockito.mock(BytesMessage.class);
        when(queueSession.createBytesMessage()).thenReturn(bytesMessage);
        when(queueConnection.createQueueSession(anyBoolean(), anyInt())).thenReturn(queueSession);
        when(queueSession.createSender(eq(queue))).thenReturn(queueSender);
        Message message = new MimeMessage(javamail);
        message.setSubject("Subject");
        message.setFrom(new InternetAddress("from@server.nowhere"));
        message.setContent("Body", "plain/text");
        Properties properties = new Properties();
        properties.setProperty("mail.smtpjms.dstProtocol", "smtps");
        SmtpJmsTransport transport = new SmtpJmsTransport(Session.getInstance(properties), new URLName("jms"));
        Address[] to = new Address[]{new InternetAddress("to@server.nowhere")};
        transport.sendMessage(message, to);
        ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(bytesMessage, times(1)).writeBytes(argumentCaptor.capture());
        byte[] out = argumentCaptor.getValue();
        final ByteArrayInputStream outIs = new ByteArrayInputStream(out);
        // now try to read that JMS message
        JavaMailSessionDelegate sessionDelegate = Mockito.mock(JavaMailSessionDelegate.class);
        JavaMailJMSStatistics javaMailJMSStatistics = Mockito.mock(JavaMailJMSStatistics.class);
        JMS2JavaMail jms2JavaMail = new JMS2JavaMail(sessionDelegate, javaMailJMSStatistics);
        final BytesMessage inBytesMessage = Mockito.mock(BytesMessage.class);
        when(sessionDelegate.createMimeMessage(any(InputStream.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new MimeMessage(javamail, (InputStream) invocation.getArguments()[0]);
            }
        });
        when(inBytesMessage.readBytes(any(byte[].class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                byte[] arg = (byte[]) invocation.getArguments()[0];
                return outIs.read(arg);
            }
        });
        Transport tr2 = Mockito.mock(Transport.class);
        when(sessionDelegate.findTransport(eq("smtps"))).thenReturn(tr2);
        jms2JavaMail.onMessage(inBytesMessage);
        ArgumentCaptor<MimeMessage> mimeMessageArgumentCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        ArgumentCaptor<Address[]> addressArgumentCaptor = ArgumentCaptor.forClass(Address[].class);
        verify(tr2, times(1)).sendMessage(mimeMessageArgumentCaptor.capture(), addressArgumentCaptor.capture());
        MimeMessage m2 = mimeMessageArgumentCaptor.getValue();
        Assert.assertArrayEquals(to, addressArgumentCaptor.getValue());
        Assert.assertArrayEquals(message.getFrom(), m2.getFrom());
        Assert.assertEquals(message.getSubject(), m2.getSubject());
    }
}
