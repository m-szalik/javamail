package org.jsoftware.javamail;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.openmbean.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ejb bean collecting JavaMail usage statistics and expose them via JMX.
 * @author szalik
 */
@Singleton(name = "JavaMailJMSStatisticsLocal")
@Startup
public class JavaMailJMSStatistics extends NotificationBroadcasterSupport implements JavaMailJMSStatisticsLocal, JavaMailJMSStatisticsMXBean {
    private MBeanServer platformMBeanServer;
    private ObjectName objectName = null;
    private Date startDate;
    private MessageAndAddresses lastSuccessMessage, lastFailMessage;
    private final AtomicLong successCounter = new AtomicLong(0), failureCounter = new AtomicLong(0), seq = new AtomicLong(0);

    @PostConstruct
    public void registerInJMX() {
        try {
            reset();
            objectName = new ObjectName("JavaMailJMS:type=" + getClass().getName());
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            platformMBeanServer.registerMBean(this, objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Problem during registration of JavaMailStatistics into JMX:" + e, e);
        }
    }

    @PreDestroy
    public void unregisterFromJMX() {
        try {
            platformMBeanServer.unregisterMBean(this.objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Problem during unregistration of JavaMailStatistics from JMX:" + e, e);
        }
    }

    public void onSuccess(MimeMessage mimeMessage, Address[] addresses) {
        MessageAndAddresses maa = new MessageAndAddresses(mimeMessage, addresses, null);
        successCounter.incrementAndGet();
        lastSuccessMessage = maa;
        sendNotification(maa);
    }


    public void onFailure(MimeMessage mimeMessage, Address[] addresses, Exception ex) {
        MessageAndAddresses maa = new MessageAndAddresses(mimeMessage, addresses, ex);
        failureCounter.incrementAndGet();
        lastFailMessage = maa;
        sendNotification(maa);
    }


    @Override
    public Date getStatisticsCollectionStartDate() {
        return startDate;
    }

    @Override
    public CompositeData getLastSentMailInfo() {
        return convert(lastSuccessMessage);
    }

    @Override
    public CompositeData getLastFailureMailInfo() {
        CompositeData cd = convert(lastFailMessage);
        return cd;
    }

    @Override
    public long getEmailsSentSuccessCounter() {
        return successCounter.get();
    }

    @Override
    public long getEmailsSentFailureCounter() {
        return failureCounter.get();
    }

    @Override
    public void reset() {
        startDate = new Date();
        lastFailMessage = null;
        lastSuccessMessage = null;
        successCounter.set(0);
        failureCounter.set(0);
    }


    private void sendNotification(MessageAndAddresses maa) {
        try {
            Notification notification;
            if (maa.getException() == null) {
                notification = new Notification("JavaMail-Send-Success", maa.getMessage().getMessageID(), seq.incrementAndGet(), "Sent");
            } else {
                notification = new Notification("JavaMail-Send-Failure", maa.getMessage().getMessageID(), seq.incrementAndGet(), "Exception:" + maa.getException());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Subject:").append(maa.getMessage().getSubject()).append('\n');
            for(Address address : maa.getAddresses()) {
                sb.append(address).append('\n');
            }
            notification.setUserData(sb.toString());
            sendNotification(notification);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static CompositeData convert(MessageAndAddresses maa) {
        if (maa == null) {
            return null;
        }
        try {
            CompositeType rowHeaderType = new CompositeType("MailHeaders", "Mail headers",
                    new String[]{"header-name", "header-value"},
                    new String[]{"Name", "Value"},
                    new OpenType[]{SimpleType.STRING, SimpleType.STRING}
            );

            CompositeType rowAddrType = new CompositeType("MailAddress", "Mail single address",
                    new String[]{"addressType", "address"},
                    new String[]{"Address type", "Email address"},
                    new OpenType[]{SimpleType.STRING, SimpleType.STRING}
            );
            TabularType tabAddressType = new TabularType("Addresses", "Mail addresses", rowAddrType, new String[] {"addressType", "address"});
            TabularType tabHeadersType = new TabularType("Headers", "Mail headers", rowHeaderType, new String[] {"header-name"});

            CompositeType retType = new CompositeType("MailInfo", "Mail info",
                    new String[]{"messageId", "date", "subject", "toAddresses", "headers", "errorDescription"},
                    new String[]{"Message ID", "Sent date", "Message subject", "Table of addresses", "Message headers", "Error description if any"},
                    new OpenType[]{SimpleType.STRING, SimpleType.DATE, SimpleType.STRING, tabAddressType, tabHeadersType, SimpleType.STRING}
            );
            TabularData addrData = new TabularDataSupport(tabAddressType);
            for(Address addr : maa.getAddresses()) {
                addrData.put(new CompositeDataSupport(rowAddrType, new String[]{"addressType", "address"}, new Object[]{addr.getType(), addr.toString()}));
            }
            TabularData headerData = new TabularDataSupport(tabHeadersType);
            Enumeration en = maa.getMessage().getAllHeaders();
            while (en.hasMoreElements()) {
                Header header = (Header) en.nextElement();
                headerData.put(new CompositeDataSupport(rowHeaderType, new String[]{"header-name", "header-value"}, new Object[]{header.getName(), header.getValue()}));
            }
            String error = null;
            if (maa.getException() != null) {
                StringWriter sw = new StringWriter();
                sw.append(maa.getException().toString());
                maa.getException().printStackTrace(new PrintWriter(sw));
                sw.flush();
                error = sw.toString();
            }
            return new CompositeDataSupport(retType,
                    new String[] {"messageId", "date", "subject", "toAddresses", "headers", "errorDescription"},
                    new Object[]{maa.getMessage().getMessageID(), new Date(maa.getTimestamp()), maa.getMessage().getSubject(), addrData, headerData, error}
            );
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }
}


class MessageAndAddresses {
    private final MimeMessage message;
    private final Address[] addresses;
    private final Exception exception;
    private final long timestamp;

    MessageAndAddresses(MimeMessage message, Address[] addresses, Exception exception) {
        this.message = message;
        this.addresses = addresses;
        this.exception = exception;
        this.timestamp = System.currentTimeMillis();
    }

    public Exception getException() {
        return exception;
    }

    public MimeMessage getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Address[] getAddresses() {
        return addresses;
    }
}