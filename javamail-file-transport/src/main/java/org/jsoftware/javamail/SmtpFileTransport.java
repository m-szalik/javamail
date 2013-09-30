package org.jsoftware.javamail;

import javax.mail.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * {@link Transport} that saves messages to file using different format witters
 * @author szalik
 */
public class SmtpFileTransport extends Transport {
	private final File dir;


	public SmtpFileTransport(Session session, URLName urlname) {
		super(session, urlname);
		String s = session.getProperties().getProperty("mail.smtpfile.path", ".");
		dir = new File(s);
		if (! dir.exists()) {
			if (! dir.mkdirs()) {
				throw new IllegalArgumentException("Unable to create output directory " + dir.getAbsolutePath());
			}
		}
	}


	@Override
	public void sendMessage(Message message, Address[] addresses) throws MessagingException {
		UUID uuid = UUID.randomUUID();
        validate(message, addresses);
		File file = new File(dir, uuid + ".msg");
		try {
			FileOutputStream fw = new FileOutputStream(file);
			try {
				message.writeTo(fw);
			} finally {
				fw.close();
			}
		} catch (IOException e) {
			throw new MessagingException("Failed to write file " + file.getAbsolutePath(), e);
		}
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
	public synchronized void close() throws MessagingException {
		// do nothing
	}


    private void validate(Message message, Address[] addresses) throws MessagingException {
        if (message.getFrom() == null || message.getFrom().length == 0) {
            throw new MessagingException("No FROM address set!");
        }
        if (addresses.length == 0) {
            throw new MessagingException("No RECIPIENTS set!");
        }
    }

}
