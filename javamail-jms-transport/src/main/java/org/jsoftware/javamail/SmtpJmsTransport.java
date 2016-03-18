package org.jsoftware.javamail;


import javax.jms.*;
import javax.mail.*;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.event.TransportEvent;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends {@link Message}s to JMS {@link Queue}.
 * Additional message headers are supported:
 * <ul>
 *     <li><b>X-Send-expire</b> = is translated to JMS TTL</li>
 *     <li><b>X-Send-priority</b> = is translated to JMS priority (allowed number also <i>normal</i>, <i>high</i> and <i>low</i></li>
 * </ul>
 * 
 * @see javax.jms.MessageProducer#setTimeToLive(long)
 * @see javax.jms.Message#setJMSPriority(int)
 * @see #createJmsMessage(javax.jms.QueueSession, javax.mail.Message, javax.mail.Address[])
 * @author szalik
 */
public class SmtpJmsTransport extends Transport {
	private static final Logger logger = Logger.getLogger(SmtpJmsTransport.class.getName());
	private final static Address[] ADDRESSES_EMPTY = new Address[0];
	static final String X_SEND_PRIORITY = "X-Send-priority";
	static final String X_PRIORITY = "X-Priority";
	static final String X_SEND_EXPIRE = "X-Send-expire";
	private final QueueConnectionFactory queueConnectionFactory;
	private final Queue mailQueue;
	private final boolean validateFrom;
	private final String protocolToUse;
    private final int jmsDeliveryMode;

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
			throw new IllegalArgumentException("No provider for dstProtocol (" + protocolToUse + ")", e);
		}
        this.jmsDeliveryMode = Integer.parseInt(getProperty(session, "jmsDeliveryMode", Integer.toString(DeliveryMode.PERSISTENT)));
	}

	
	private static String getProperty(Session session, String keySuffix, String defaultValue) {
		return session.getProperties().getProperty("mail.smtpjms." + keySuffix, defaultValue);
	}


	@Override
	public void sendMessage(Message msg, Address[] addresses) throws MessagingException {
		if (validateFrom && (msg.getFrom() == null || msg.getFrom().length == 0)) {
			fail(msg, addresses, "Field FROM not set or empty!");
		}
        if (addresses.length == 0) {
            fail(msg, addresses, "No RECIPIENTS set!");
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
					logger.warning("Error setting JMS TTL. Value='" + str[0] + "'" + e);
				}
			}
			queueConnection.start();
			queueSender.setDeliveryMode(jmsDeliveryMode);
			queueSender.send(createJmsMessage(queueSession, msg, addresses));
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Message " + msg.getMessageNumber() + " sent to JMS queue.");
            }
            notifyTransportListeners(TransportEvent.MESSAGE_DELIVERED, addresses, ADDRESSES_EMPTY, ADDRESSES_EMPTY, msg);
        } catch(JMSException ex) {
            notifyTransportListeners(TransportEvent.MESSAGE_NOT_DELIVERED, ADDRESSES_EMPTY, addresses, ADDRESSES_EMPTY, msg);
			throw new MessagingException("Cannot send message " + msg.toString() + " JMS queue.", ex);
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

	private void fail(Message message, Address[] addresses, String msg) throws MessagingException {
		notifyTransportListeners(TransportEvent.MESSAGE_NOT_DELIVERED, ADDRESSES_EMPTY, addresses, ADDRESSES_EMPTY, message);
		throw new MessagingException(msg);
	}


    @Override
    protected boolean protocolConnect(String host, int port, String user, String password) throws MessagingException {
        return true;
    }

    /**
     * Create javax.jms.Message for javax.mail.Message
     * JMS message contains:
     * <ul>
     *  <li>protocolToUse (String) - destination / final Transport</li>
     *  <li>addresses (array javax.mail.Address) - who to send to</li>
     *  <li>message (object javax.mail.Message) - message content</li>
     * </ul>
     */
    private javax.jms.Message createJmsMessage(QueueSession queueSession, Message msg, Address[] addresses) throws JMSException, MessagingException {
		BytesMessage jms = queueSession.createBytesMessage();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeUTF(protocolToUse);
			oos.writeObject(addresses == null ? new Address[0] : addresses);
			msg.writeTo(oos);
		} catch (IOException e) {
			throw new MessagingException("Could not send JMS message with mail content for Message-ID:" + msg.getHeader("Message-ID"), e);
		}
		jms.writeBytes(baos.toByteArray());
		Integer priority = jmsPriority(msg);
        if (priority != null && priority >= 0) {
            jms.setJMSPriority(priority);
        }
		return jms;
	}

	/**
	 * @param msg message
	 * @return jms priority or null if default
	 */
	static Integer jmsPriority(Message msg) throws MessagingException {
		Integer priority = null;
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
		} else { // if no "X-Send-priority" check for mail priority "X-Priority"
			str = msg.getHeader(X_PRIORITY);
			if (str != null && str.length > 0) {
				try {
					int xPriority = Integer.parseInt(str[0]);
					switch (xPriority) {
						case 1:
							priority = 8;
							break;
						case 2:
							priority = 6;
							break;
						case 4:
							priority = 4;
							break;
						case 5:
							priority = 1;
							break;
						default:
							logger.warning("Unmapped value for \"" + X_PRIORITY + "\" header: \"" + str[0] + '"');
							break;
					}
				} catch (NumberFormatException ex) {
					logger.warning("Invalid \"" + X_PRIORITY + "\" header value '" + str[0] + "'!");
				}
			}
		}
		return priority;
	}
}
