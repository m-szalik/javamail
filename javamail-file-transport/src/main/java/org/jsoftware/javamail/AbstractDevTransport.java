package org.jsoftware.javamail;

import javax.mail.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    static void validate(Message message, Address[] addresses) throws MessagingException {
        if (message.getFrom() == null || message.getFrom().length == 0) {
            throw new MessagingException("No FROM address set!");
        }
        if (addresses.length == 0) {
            throw new MessagingException("No RECIPIENTS set!");
        }
    }


    /**
     * Extract parts from Multipart message.
     * @param multipart
     * @return map of part contentType -> part content
     * @throws MessagingException
     * @throws IOException
     */
    static Map<String,String> extractTextParts(Multipart multipart) throws MessagingException, IOException {
        HashMap<String,String> bodies = new HashMap<String,String>();
        for(int i = 0; i < multipart.getCount(); i++) {
            checkPart(bodies, multipart.getBodyPart(i));
        }
        return bodies;
    }



    private static void checkPart(HashMap<String, String> bodies, Part part) throws IOException, MessagingException {
        Object content = part.getContent();
        if (content instanceof CharSequence) {
            bodies.put(part.getContentType(), content.toString());
        } else if (content instanceof Multipart) {
            Multipart mp = (Multipart) content;
            for(int i = 0; i < mp.getCount(); i++) {
                checkPart(bodies, mp.getBodyPart(i));
            }
        }
    }

}
