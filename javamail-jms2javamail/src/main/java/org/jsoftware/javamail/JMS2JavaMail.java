package org.jsoftware.javamail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

@MessageDriven(
		mappedName = "jms/mailQueue", name = "mailQueue"
)
public class JMS2JavaMail implements MessageListener {
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	@Resource(mappedName="mail/serverSession")
	private Session session;

	
	
	public void onMessage(Message message) {
		logger.info("Recieved message " + message);
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
				MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(baos.toByteArray()));
				Address[] addresses = (Address[]) bmsg.getObjectProperty("addresses");
				session.getTransport().sendMessage(mimeMessage, addresses);
				ack(message);
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Error processing JMS message.", ex);
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
