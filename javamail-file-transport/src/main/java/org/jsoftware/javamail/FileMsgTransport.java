package org.jsoftware.javamail;

import javax.mail.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link Transport} Saves whole mail as msg (mbox format)
 * @author szalik
 */
public class FileMsgTransport extends AbstractFileTransport {

	public FileMsgTransport(Session session, URLName urlname) {
		super(session, urlname);
	}


    @Override
    protected void writeMessage(Message message, Address[] addresses, OutputStream os) throws IOException, MessagingException {
        message.writeTo(os);
    }


    @Override
    protected String getFilenameExtension() {
        return "msg";
    }
}
