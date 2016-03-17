package org.jsoftware.javamail;

import javax.mail.*;
import javax.mail.event.TransportEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link javax.mail.Transport} that saves messages to file using different format writers
 * @author szalik
 */
abstract class AbstractFileTransport extends AbstractDevTransport {
    private final static Address[] ADDRESSES_EMPTY = new Address[0];
	private final File directory;


	AbstractFileTransport(Session session, URLName urlname) {
		super(session, urlname);
		String s = session.getProperties().getProperty("mail.files.path", ".");
		directory = new File(s);
		if (! directory.exists()) {
			if (! directory.mkdirs()) {
				throw new IllegalArgumentException("Unable to create output directory " + directory.getAbsolutePath());
			}
		}
	}


	@Override
	public void sendMessage(Message message, Address[] addresses) throws MessagingException {
		validateAndPrepare(message, addresses);
        File file = null;
        String base = Long.toString(System.currentTimeMillis(), 32);
        try {
            synchronized (this) {
                int suffix = -1;
                do {
                    suffix++;
                    StringBuilder sb = new StringBuilder(base);
                    if (suffix > 0) {
                        sb.append('-').append(suffix);
                    }
                    sb.append('.').append(getFilenameExtension());
                    file = new File(directory, sb.toString());
                } while (file.exists());
                if (! file.createNewFile()) {
                    throw new IOException("Cannot create file " + file.getAbsolutePath());
                }
            }
            FileOutputStream fw = null;
			try {
                fw = new FileOutputStream(file);
				writeMessage(message, fw);
                notifyTransportListeners(TransportEvent.MESSAGE_DELIVERED, message.getAllRecipients(), ADDRESSES_EMPTY, ADDRESSES_EMPTY, message);
			} finally {
                if (fw != null) {
				    fw.close();
                }
			}
		} catch (IOException e) {
            notifyTransportListeners(TransportEvent.MESSAGE_NOT_DELIVERED, ADDRESSES_EMPTY, message.getAllRecipients(), ADDRESSES_EMPTY, message);
			throw new MessagingException("Failed to write file " + (file.getAbsolutePath()), e);
		}
	}


    /**
     * Write message
     * @param message message
     * @param os where to write
     * @throws IOException when message generation error occurs
     * @throws MessagingException when sending message error occurs
     */
    protected abstract void writeMessage(Message message, OutputStream os) throws IOException, MessagingException;


    /**
     * @return filename extension (file where mail is saved to)
     */
    protected abstract String getFilenameExtension();

}
