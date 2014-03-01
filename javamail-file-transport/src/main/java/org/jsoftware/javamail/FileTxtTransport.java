package org.jsoftware.javamail;

import javax.mail.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

/**
 * {@link javax.mail.Transport} Saves mail as text - only headers and text part of multi-part is saved
 * @author szalik
 * @since 1.5.1
 */
public class FileTxtTransport extends AbstractFileTransport {
    private final static List<String> HEADERS_ORDER = Arrays.asList("Date", "From", "To", "Message-ID", "Subject"); // than others

	public FileTxtTransport(Session session, URLName urlname) {
		super(session, urlname);
	}

    @Override
    protected void writeMessage(Message message, Address[] addresses, OutputStream os) throws IOException, MessagingException {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(os, "UTF-8");
            // Ordered headers
            for(String header : HEADERS_ORDER) {
                addHeader(message, header, writer);
            }
            // Other headers
            Enumeration<?> en = message.getAllHeaders();
            while (en.hasMoreElements()) {
                String header = ((Header) en.nextElement()).getName();
                if (skipHeader(header)) {
                    continue;
                }
                addHeader(message, header, writer);
            }
            // Body text version
            writer.append('\n');
            Object content = message.getContent();
            String body = null;
            if (content instanceof Multipart) {
                HashMap<String,String> bodies = new HashMap<String,String>();
                Multipart mp = (Multipart) content;
                for(int i = 0; i < mp.getCount(); i++) {
                    checkPart(bodies, mp.getBodyPart(i));
                }
                for(Map.Entry<String,String> me : bodies.entrySet()) {
                    String key = me.getKey().toLowerCase();
                    if (key.startsWith("text/plain")) {
                        body = me.getValue();
                        break;
                    }
                    if (key.startsWith("text")) {
                        body = me.getValue();
                    }
                }
            } else {
                body = content.toString();
            }
            if (body == null) {
                writer.append("UNABLE TO FIND BODY (text nor html)!");
            } else {
                writer.append(body);
            }
            writer.append('\n');
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }


    private boolean skipHeader(String header) {
        for(String h : HEADERS_ORDER) {
            if (h.equalsIgnoreCase(header)) {
                return true;
            }
        }
        return false;
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


    private static void addHeader(Message message, String header, Writer writer) throws MessagingException, IOException {
        for(String hv : message.getHeader(header)) {
            if (hv != null && hv.trim().length() > 0) {
                writer.append(header).append(": ").append(hv).append('\n');
            }
        }
    }


    @Override
    protected String generateFilename(Message message, Address[] addresses) {
        return UUID.randomUUID().toString() + ".txt";
    }
}
