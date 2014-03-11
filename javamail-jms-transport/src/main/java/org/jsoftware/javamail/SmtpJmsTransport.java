package org.jsoftware.javamail;


import javax.jms.*;
import javax.mail.*;
import javax.mail.Message;
import javax.mail.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends {@link Message}s to JMS {@link Queue}.
 * <p>Additional message headers are supported:
 * <ul>
 *     <li><b>X-Send-expire</b> = is translated to JMS TTL</li>
 *     <li><b>X-Send-priority</b> = is translated to JMS priority (allowed number also <i>normal</i>, <i>high</i> and <i>low</i></li>
 * </ul>
 * </p>
 * @see javax.jms.MessageProducer#setTimeToLive(long)
 * @see javax.jms.Message#setJMSPriority(int)
 * @author szalik
 */
public class SmtpJmsTransport extends Transport {
	private static final String X_SEND_PRIORITY = "X-Send-priority";
	private static final String X_SEND_EXPIRE = "X-Send-expire";
	private final Logger logger = Logger.getLogger(getClass().getName());
	private final QueueConnectionFactory queueConnectionFactory;
	private final Queue mailQueue;
	private final boolean validateFrom;
	private final String protocolToUse;
	
	public SmtpJmsTransport(Session session, URLName urlname) {
		super(session, urlname);
		try {
			InitialContext initialContext = new InitialContext();
			queueConnectionFactory = (QueueConnectionFactory) initialContext.lookup(getProperty(session, "jmsQueueConnectionFactory", "jms/queueConnectionFactory"));
			mailQueue = (Queue) initialContext.lookup(getProperty(session, "jmsQueue", "jms/mailQueue"));
		} catch (NamingException e) {
			throw new RuntimeException("Cannot create SmtpJmsTransport.", e);
		}
		String str = getProperty(session, "validateFrom", "true");
		validateFrom = Boolean.valueOf(str);
		protocolToUse = getProperty(session, "dstProtocol", "smtp");
		// Check if dstProtocol is available.
		try {
			session.getTransport(protocolToUse);
		} catch (NoSuchProviderException e) {
			throw new RuntimeException("No provider for dstProtocol (" + protocolToUse + ")", e);
		}
	}

	
	private static String getProperty(Session session, String keySuffix, String defaultValue) {
		return session.getProperties().getProperty("mail.smtpjms." + keySuffix, defaultValue);
	}


	@Override
	public void sendMessage(Message msg, Address[] addresses) throws MessagingException {
		if (validateFrom && (msg.getFrom() == null || msg.getFrom().length == 0)) {
			throw new MessagingException("Field FROM not set or empty!");
		}
        if (addresses.length == 0) {
            throw new MessagingException("No RECIPIENTS set!");
        }
		QueueConnection queueConnection = null;
		QueueSession queueSession = null;
		try {
			queueConnection = queueConnectionFactory.createQueueConnection();
			queueSession = queueConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
			QueueSender queueSender = queueSession.createSender(mailQueue);
			String[] str = msg.getHeader(X_SEND_EXPIRE);
			if (str != null && str.length > 0) {
				try {
					queueSender.setTimeToLive(Long.parseLong(str[0]));
				} catch(Exception e) {
					logger.warning("Error setting JMS TTL." + e);
				}
			}
			queueConnection.start();
			queueSender.setDeliveryMode(DeliveryMode.PERSISTENT);
			queueSender.send(createJmsMessage(queueSession, msg, addresses));
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Message " + msg.getMessageNumber() + " sent to JMS queue.");
            }
        } catch(JMSException ex) {
			throw new MessagingException("Cannot send message " + msg + " JMS queue.", ex);
		} finally {
			try {
                if (queueSession != null) {
                    queueSession.close();
                }
            } catch(JMSException jmsEx)	{
                logger.warning("Problem closing JMS session - " + jmsEx);
            }
			try {
                if (queueConnection != null) {
                    queueConnection.close();
                }
            } catch(JMSException jmsEx)	{
                logger.warning("Problem closing JMS connection - " + jmsEx);
            }
		}
	}


    @Override
    protected boolean protocolConnect(String host, int port, String user, String password) throws MessagingException {
        return true;
    }


    private javax.jms.Message createJmsMessage(QueueSession queueSession, Message msg, Address[] addresses) throws JMSException, MessagingException {
		int priority = 5;    // we use 5 as normal JMS priority
		String[] str = msg.getHeader(X_SEND_PRIORITY);
		if (str != null && str.length > 0) {
			msg.removeHeader(X_SEND_PRIORITY);
			try {
				if("low".equalsIgnoreCase(str[0])) {
					priority = 1;
				} else if ("high".equalsIgnoreCase(str[0])) {
					priority = 8;
				} else if (! "normal".equalsIgnoreCase(str[0])) {
                    priority = Integer.parseInt(str[0]);
					priority = Math.max(priority, 0);
					priority = Math.min(priority, 9);
				}
			} catch(NumberFormatException nfe) {
				logger.warning("Invalid value for " + X_SEND_PRIORITY + " - " + str[0]);
			}
		}
		BytesMessage jms = queueSession.createBytesMessage();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeUTF(protocolToUse);
			oos.writeObject(addresses == null ? new Address[0] : addresses);
			msg.writeTo(oos);
		} catch (IOException e) {
			MessagingException mex = new MessagingException();
			mex.initCause(e);
			throw mex;
		}
		jms.writeBytes(baos.toByteArray());
		jms.setJMSPriority(priority);
		return jms;
	}
}
