package org.jsoftware.javamail;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.event.TransportEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link javax.mail.Transport} that saves messages to file using different format writers
 * @author szalik
 */
abstract class AbstractFileTransport extends AbstractTransport {
    private final static Address[] ADDRESSES_EMPTY = new Address[0];
	private final File directory;


	AbstractFileTransport(Session session, URLName urlname) {
		super(session, urlname);
		String s = session.getProperties().getProperty("mail.files.path", ".");
		directory = new File(s);
		if (! directory.exists() && ! directory.mkdirs()) {
            throw new IllegalArgumentException("Unable to create output directory " + directory.getAbsolutePath());
		}
	}

    File getDirectory() {
        return directory;
    }

    @Override
	public void sendMessage(Message message, Address[] addresses) throws MessagingException {
		validateAndPrepare(message, addresses);
        FileOutputStream fw = null;
        File file = null;
        try {
            String baseName = Long.toString(System.currentTimeMillis(), 32);
            file = createMessageFile(baseName);
            fw = new FileOutputStream(file);
            writeMessage(message, fw);
            notifyTransportListeners(TransportEvent.MESSAGE_DELIVERED, message.getAllRecipients(), ADDRESSES_EMPTY, ADDRESSES_EMPTY, message);
		} catch (IOException e) {
            notifyTransportListeners(TransportEvent.MESSAGE_NOT_DELIVERED, ADDRESSES_EMPTY, message.getAllRecipients(), ADDRESSES_EMPTY, message);
            if (file == null) {
                throw new MessagingException("Cennot create message file", e);
            } else {
                throw new MessagingException("Failed to write file " + file.getAbsolutePath(), e);
            }
		} finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex)  {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot close " + fw, ex);
                }
            }
        }
	}

    synchronized File createMessageFile(String baseName) throws IOException {
        int suffix = -1;
        File file;
        do {
            suffix++;
            StringBuilder sb = new StringBuilder(baseName);
            if (suffix > 0) {
                sb.append('-').append(suffix);
            }
            sb.append('.').append(getFilenameExtension());
            file = new File(directory, sb.toString());
        } while (file.exists());
        if (! file.createNewFile()) {
            throw new IOException("Cannot create file " + file.getAbsolutePath());
        }
        return file;
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
