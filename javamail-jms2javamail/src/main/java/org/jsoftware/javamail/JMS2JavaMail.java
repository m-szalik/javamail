package org.jsoftware.javamail;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get javax.mail.Message (bytes) from JMS queue and send it using required Transport.
 * <p>A required protocol is a part of JMS message</p>
 * See SmtpJmsTransport class
 */
@MessageDriven(mappedName = "jms/mailQueue", name = "mailQueue")
public class JMS2JavaMail implements MessageListener {
	private final Logger logger = Logger.getLogger(getClass().getName());
	private JavaMailSessionDelegate javaMailSessionDelegate;

    /** Mail session to be used to send a message */
	@Resource(mappedName="mail/Session")
	private Session session;

    @EJB
    private JavaMailJMSStatisticsLocal javaMailJMSStatisticsLocal;

	public JMS2JavaMail() {
	}

	/* For testing */
	JMS2JavaMail(JavaMailSessionDelegate javaMailSessionDelegate, JavaMailJMSStatistics javaMailJMSStatistics) {
		this.javaMailSessionDelegate = javaMailSessionDelegate;
		this.javaMailJMSStatisticsLocal = javaMailJMSStatistics;
	}


	public void onMessage(Message message) {
		if (message instanceof BytesMessage) {
			String protocolToUse = null;
			try {
				BytesMessage bMsg = (BytesMessage) message;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[512];
				int r;
				do {
					r = bMsg.readBytes(buf);
					if (r < 0) {
                        break;
                    }
					baos.write(buf, 0, r);
				} while (true);
				ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
				ObjectInputStream ois = new ObjectInputStream(bais);
				protocolToUse = ois.readUTF();
				Address[] addresses = (Address[]) ois.readObject();
				MimeMessage mimeMessage = javaMailSessionDelegate.createMimeMessage(ois);
                try {
                    Transport transport = javaMailSessionDelegate.findTransport(protocolToUse);
                    if (transport.isConnected()) {
                        transport.sendMessage(mimeMessage, addresses);
                    } else {
                        try {
                            transport.connect();
                            transport.sendMessage(mimeMessage, addresses);
                        } finally {
                            transport.close();
                        }
                    }
                    ack(message);
                    javaMailJMSStatisticsLocal.onSuccess(mimeMessage, addresses);
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, "Message " + mimeMessage.getMessageID() + " (" + mimeMessage.getMessageNumber() + ") successfully sent to destination javaMailSession using " + protocolToUse + " -> " + transport + ".");
                    }
                } catch (MessagingException mEx) {
                    javaMailJMSStatisticsLocal.onFailure(mimeMessage, addresses, mEx);
                    throw mEx;
                }
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Error processing JMS message - " + message + ". Protocol: " + protocolToUse, ex);
			}
		} else {
			logger.warning("Found message that is not BytesMessage - " + message + ", " + message.getClass());
			ack(message);
		}
	}

	@PostConstruct
	public void init() {
		if (session == null) {
			throw new IllegalStateException("JavaMail session is null.");
		}
		javaMailSessionDelegate = new JavaMailSessionDelegate() {
			@Override
			public Transport findTransport(String protocolToUse) throws NoSuchProviderException {
				return session.getTransport(protocolToUse);
			}
			@Override
			public MimeMessage createMimeMessage(InputStream inputStream) throws MessagingException {
				return new MimeMessage(session, inputStream);
			}
		};
	}

	private void ack(Message message) {
		try {
			message.acknowledge();
		} catch(JMSException e) { 
			logger.warning("Error acknowledging " + message + ":: " + e);
		}
	}	
	
}
