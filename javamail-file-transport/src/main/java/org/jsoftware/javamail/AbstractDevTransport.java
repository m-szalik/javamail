package org.jsoftware.javamail;

import javax.mail.*;

/**
 * {@link javax.mail.Transport} for developers
 * @author szalik
 */
public abstract class AbstractDevTransport extends Transport {

	public AbstractDevTransport(Session session, URLName urlname) {
		super(session, urlname);
	}

    @Override
	public void connect(String host, int port, String user, String password) throws MessagingException {
		// do nothing
	}


	@Override
	public boolean isConnected() {
		return true;
	}


	@Override
	public void close() throws MessagingException {
		// do nothing
	}


    /**
     * Validate FROM and RECIPIENTS
     * @param message
     * @param addresses
     * @throws MessagingException
     */
    protected void validate(Message message, Address[] addresses) throws MessagingException {
        if (message.getFrom() == null || message.getFrom().length == 0) {
            throw new MessagingException("No FROM address set!");
        }
        if (addresses.length == 0) {
            throw new MessagingException("No RECIPIENTS set!");
        }
    }

}
