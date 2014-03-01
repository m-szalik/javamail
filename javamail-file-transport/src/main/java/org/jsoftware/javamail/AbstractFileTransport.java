package org.jsoftware.javamail;

import javax.mail.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link javax.mail.Transport} that saves messages to file using different format writers
 * @author szalik
 */
public abstract class AbstractFileTransport extends AbstractDevTransport {
	private final File dir;


	public AbstractFileTransport(Session session, URLName urlname) {
		super(session, urlname);
		String s = session.getProperties().getProperty("mail.files.path", ".");
		dir = new File(s);
		if (! dir.exists()) {
			if (! dir.mkdirs()) {
				throw new IllegalArgumentException("Unable to create output directory " + dir.getAbsolutePath());
			}
		}
	}


	@Override
	public void sendMessage(Message message, Address[] addresses) throws MessagingException {
		validate(message, addresses);
        File file = new File(dir, generateFilename(message, addresses));
		try {
			FileOutputStream fw = null;
			try {
                fw = new FileOutputStream(file);
				writeMessage(message, addresses, fw);
			} finally {
                if (fw != null) {
				    fw.close();
                }
			}
		} catch (IOException e) {
			throw new MessagingException("Failed to write file " + file.getAbsolutePath(), e);
		}
	}

    /**
     * Write message
     * @param message message
     * @param os where to write
     * @throws IOException
     * @throws MessagingException
     */
    protected abstract void writeMessage(Message message, Address[] addresses, OutputStream os) throws IOException, MessagingException;

    /**
     * Generate filename for a message
     * @param message message
     * @param addresses addresses
     * @return filename
     */
    protected abstract String generateFilename(Message message, Address[] addresses);

}
