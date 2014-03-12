package org.jsoftware.javamail;

import javax.mail.*;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * This transport only adds info about an email to logs.
 * @author szalik
 * @since 1.5.1
 */
public class NopTransport extends AbstractDevTransport {
    private final Logger logger = Logger.getLogger(getClass().getName());


    public NopTransport(Session session, URLName urlname) {
        super(session, urlname);
    }



    @Override
    public void sendMessage(Message msg, Address[] addresses) throws MessagingException {
        validateAndPrepare(msg, addresses);
        logger.info("Message {subject=" + msg.getSubject() + ", to=" + Arrays.asList(addresses) + "}");
    }

}
