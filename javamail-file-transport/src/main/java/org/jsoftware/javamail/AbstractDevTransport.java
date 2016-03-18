package org.jsoftware.javamail;

import javax.mail.*;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.TransportEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * {@link javax.mail.Transport} for developers
 * @author szalik
 */
abstract class AbstractDevTransport extends Transport {

	AbstractDevTransport(Session session, URLName urlname) {
		super(session, urlname);
	}

    @Override
	public void connect(String host, int port, String user, String password) throws MessagingException {
		// do nothing because we do not need to connect anywhere
        notifyConnectionListeners(ConnectionEvent.OPENED);
	}

	@Override
	public boolean isConnected() {
		return true; //it's always connected to local file system :)
	}

	@Override
	public void close() throws MessagingException {
		// do nothing, we didn't have to connect so we also do not have to disconnect.
        notifyConnectionListeners(ConnectionEvent.DISCONNECTED);
        notifyConnectionListeners(ConnectionEvent.CLOSED);
	}

    private void fail(Message message, Address[] addresses, String msg) throws MessagingException {
        Address[] empty = new Address[0];
        notifyTransportListeners(TransportEvent.MESSAGE_NOT_DELIVERED, empty, addresses, empty, message);
        throw new MessagingException(msg);
    }

    /**
     * Validate FROM and RECIPIENTS
     * @param message message to validate
     * @param addresses addresses to be added into message if not duplicated
     * @throws MessagingException
     */
    void validateAndPrepare(Message message, Address[] addresses) throws MessagingException {
        if (message.getFrom() == null || message.getFrom().length == 0) {
            fail(message, addresses, "No FROM address set!");
        }
        if (addresses.length == 0) {
            fail(message, addresses, "No RECIPIENTS set!");
        }
        Address[] messageAddresses = message.getAllRecipients();
        if (messageAddresses == null) {
            for(Address address : addresses) {
                message.addRecipient(Message.RecipientType.TO, address);
            }
        } else {
            out:
            for(Address address : addresses) {
                for(Address a : messageAddresses) {
                    if (a == null) {
                        continue;
                    }
                    if (a.equals(address)) {
                        continue out;
                    }
                }
                message.addRecipient(Message.RecipientType.TO, address);
            }
        }
    }

    /**
     * Extract parts from Multi-part message.
     * @param multiPart multi-part to visit
     * @return map of part contentType -> part content
     * @throws MessagingException
     * @throws IOException
     */
    static Map<String,Collection<String>> extractTextParts(Multipart multiPart) throws MessagingException, IOException {
        HashMap<String,Collection<String>> bodies = new HashMap<>();
        for(int i = 0; i < multiPart.getCount(); i++) {
            checkPartForTextType(bodies, multiPart.getBodyPart(i));
        }
        return bodies;
    }


    /**
     * Recursive find body parts and save them to HashMap
     */
    private static void checkPartForTextType(HashMap<String, Collection<String>> bodies, Part part) throws IOException, MessagingException {
        Object content = part.getContent();
        if (content instanceof CharSequence) {
            String ct = part.getContentType();
            Collection<String> value = bodies.get(ct);
            if (value != null) {
                value.add(content.toString());
            } else {
                value = new LinkedList<>();
                value.add(content.toString());
                bodies.put(ct, value);
            }
        } else if (content instanceof Multipart) {
            Multipart mp = (Multipart) content;
            for(int i = 0; i < mp.getCount(); i++) {
                checkPartForTextType(bodies, mp.getBodyPart(i));
            }
        }
    }

}
