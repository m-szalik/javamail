package org.jsoftware.javamail;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

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
        when(mimeMessage.getAllHeaders()).thenReturn(Collections.enumeration(Arrays.asList(new Header("h1", "v1"), new Header("h2", "v2"))));
        when(mimeMessage.getMessageID()).thenReturn("MessageId");
        when(mimeMessage.getSubject()).thenReturn("MessageSubject");
    }

    @After
    public void tearDown() throws Exception {
        javaMailJMSStatistics.unregisterFromJMX();
    }

    @Test
    public void testOnSuccess() throws Exception {
        javaMailJMSStatistics.onSuccess(mimeMessage, addressesTo);
        Assert.assertEquals(1L, getMbeanAttribute("countSuccessful"));
    }

    @Test
    public void testOnFailure() throws Exception {
        javaMailJMSStatistics.onFailure(mimeMessage, addressesTo, new IllegalArgumentException());
        Assert.assertEquals(1L, getMbeanAttribute("countFailure"));
    }

    @Test
    public void testReset() throws Exception {
        Date date = new Date();
        javaMailJMSStatistics.onSuccess(mimeMessage, addressesTo);
        javaMailJMSStatistics.invoke("reset", new Object[0], new String[0]);
        Assert.assertEquals(0L, getMbeanAttribute("countSuccessful"));
        Date d = (Date) getMbeanAttribute("statisticsCollectionStartDate");
        Assert.assertTrue(d.after(date));
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

    @Test
    public void testJmxMBeanInfo() throws Exception {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        platformMBeanServer.getMBeanInfo(JavaMailJMSStatistics.JMX_OBJECT_NAME);
        // no exceptions = ok
    }

    @Test
    public void testMessageInfo() throws Exception {
        javaMailJMSStatistics.onFailure(mimeMessage, addressesTo, new IllegalStateException());
        CompositeData info = (CompositeData) getMbeanAttribute("lastFailureMailInfo");
        Assert.assertTrue(info.get("date") instanceof Date);
        Assert.assertTrue(info.get("errorDescription").toString().contains("IllegalStateException"));
        TabularData tab = (TabularData) info.get("headers");
        Assert.assertEquals(2, tab.size());
        Assert.assertEquals("MessageId", info.get("messageId"));
        tab = (TabularData) info.get("toAddresses");
        Assert.assertEquals(1, tab.size());
        Assert.assertEquals("MessageSubject", info.get("subject"));
    }

    private static Object getMbeanAttribute(String attrName) throws Exception {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        return platformMBeanServer.getAttribute(JavaMailJMSStatistics.JMX_OBJECT_NAME, attrName);
    }
}