package org.jsoftware.javamail;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Sends {@link Message}s to JMS {@link Queue}.
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

	
	private static String getProperty(Session session, String keySufix, String defaultValue) {
		return session.getProperties().getProperty("mail.smtpjms." + keySufix, defaultValue);
	}


	@Override
	public void sendMessage(Message msg, Address[] addresses) throws MessagingException {
        if (validateFrom && (msg.getFrom() == null || msg.getFrom().length == 0)) {
			throw new MessagingException("Field from not set or empty for message " + msg);
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
			try { queueSession.close(); } catch(JMSException jmsEx) 	{	logger.warning("Problem closing JMS session - " + jmsEx);		}
			try { queueConnection.close(); } catch(JMSException jmsEx) 	{	logger.warning("Problem closing JMS connection - " + jmsEx);	}
		}
	}

	

	private javax.jms.Message createJmsMessage(QueueSession queueSession, Message msg, Address[] addresses) throws JMSException, MessagingException {
		int prio = 5;
		String[] str = msg.getHeader(X_SEND_PRIORITY);
		if (str != null && str.length > 0) {
			msg.removeHeader(X_SEND_PRIORITY);
			try {
				if("low".equals(str[0])) {
					prio = 1;
				} else if ("high".equals(str[0])) {
					prio = 8;
				} else {
					prio = Integer.parseInt(str[0]);
					prio = Math.max(prio, 0);
					prio = Math.min(prio, 9);
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
		jms.setJMSPriority(prio);
		return jms;
	}
}
