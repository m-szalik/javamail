package org.jsoftware.javamail;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import java.lang.management.ManagementFactory;

import static org.mockito.Mockito.when;

public class JavaMailJMSStatisticsTest {
    private final Address[] addressesTo;
    private JavaMailJMSStatistics javaMailJMSStatistics;
    private MimeMessage mimeMessage;

    public JavaMailJMSStatisticsTest() throws AddressException {
        addressesTo = new Address[] { new InternetAddress("nobody@nowhere.com") };
    }

    @Before
    public void setUp() throws Exception {
        javaMailJMSStatistics = new JavaMailJMSStatistics();
        javaMailJMSStatistics.registerInJMX();
        mimeMessage = Mockito.mock(MimeMessage.class);
        when(mimeMessage.getMessageID()).thenReturn("MessageId");
    }

    @After
    public void tearDown() throws Exception {
        javaMailJMSStatistics.unregisterFromJMX();
    }

    @Test
    public void testOnSuccess() throws Exception {
        javaMailJMSStatistics.onSuccess(mimeMessage, addressesTo);
        Assert.assertEquals(1, javaMailJMSStatistics.successCounter.get());
    }

    @Test
    public void testOnFailure() throws Exception {
        javaMailJMSStatistics.onFailure(mimeMessage, addressesTo, new IllegalArgumentException());
        Assert.assertEquals(1, javaMailJMSStatistics.failureCounter.get());
    }

    @Test
    public void testReset() throws Exception {
        javaMailJMSStatistics.failureCounter.set(12);
        javaMailJMSStatistics.successCounter.set(3344);
        javaMailJMSStatistics.invoke("reset", new Object[0], new String[0]);
        Assert.assertEquals(0, javaMailJMSStatistics.successCounter.get());
        Assert.assertEquals(0, javaMailJMSStatistics.failureCounter.get());
    }

    @Test
    public void testJmxListener() throws Exception {
        final StringBuilder result = new StringBuilder();
        NotificationListener listener = new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                result.append(notification.getType());
            }
        };
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        platformMBeanServer.addNotificationListener(JavaMailJMSStatistics.JMX_OBJECT_NAME, listener, null, this);
        javaMailJMSStatistics.onSuccess(mimeMessage, addressesTo);
        // cleanup
        platformMBeanServer.removeNotificationListener(JavaMailJMSStatistics.JMX_OBJECT_NAME, listener);
        // check
        Assert.assertEquals(JavaMailJMSStatistics.NOTIFICATION_TYPE_SUCCESS, result.toString());
    }
}