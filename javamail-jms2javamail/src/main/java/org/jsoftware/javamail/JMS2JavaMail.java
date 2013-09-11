package org.jsoftware.javamail;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(mappedName = "jms/mailQueue", name = "mailQueue")
public class JMS2JavaMail implements MessageListener {
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	@Resource(mappedName="mail/Session")
	private Session session;

	
	public void onMessage(Message message) {
		if (message instanceof BytesMessage) {
			try {
				BytesMessage bmsg = (BytesMessage) message;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[32];
				int r;
				do {
					r = bmsg.readBytes(buf);
					if (r < 0) break;
					baos.write(buf, 0, r);
				} while (true);
				ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
				ObjectInputStream ois = new ObjectInputStream(bais);
				String protocolToUse = ois.readUTF();
				Address[] addresses = (Address[]) ois.readObject();
				MimeMessage mimeMessage = new MimeMessage(session, ois);
				Transport transport = session.getTransport(protocolToUse);
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
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, "Message " + mimeMessage.getMessageID() + " successfuly sent to destination javaMailSession using " + protocolToUse + " -> " + transport + ".");
				}
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Error processing JMS message - " + message + ".", ex);
			}
		} else {
			logger.warning("Found message that is not BytesMessage - " + message + ", " + message.getClass());
			ack(message);
		}
	}
	
	

	private void ack(Message message) {
		try {
			message.acknowledge();
		} catch(JMSException e) { 
			logger.warning("Error acknowledging " + message + ":: " + e);
		}
	}	
	
}
