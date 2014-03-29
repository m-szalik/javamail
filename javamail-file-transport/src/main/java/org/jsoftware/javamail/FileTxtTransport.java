package org.jsoftware.javamail;

import javax.mail.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * {@link javax.mail.Transport} Saves mail as text - only headers and text part of multi-part is saved
 * @author szalik
 * @since 1.5.1
 */
public class FileTxtTransport extends AbstractFileTransport {
    private final static List<String> HEADERS_ORDER = Arrays.asList("Date", "From", "To", "Subject", "Message-ID"); // than others


	public FileTxtTransport(Session session, URLName urlname) {
		super(session, urlname);
	}


    @Override
    protected void writeMessage(Message message, OutputStream os) throws IOException, MessagingException {
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
                if (isSkipHeader(header)) {
                    continue;
                }
                addHeader(message, header, writer);
            }
            // Body text version
            writer.append('\n');
            Object content = message.getContent();
            String body = null;
            if (content instanceof Multipart) {
                for(Map.Entry<String,String> me : extractTextParts((Multipart) content).entrySet()) {
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



    private boolean isSkipHeader(String header) {
        for(String h : HEADERS_ORDER) {
            if (h.equalsIgnoreCase(header)) {
                return true;
            }
        }
        return false;
    }



    private static void addHeader(Message message, String header, Writer writer) throws MessagingException, IOException {
        String[] headers = message.getHeader(header);
        if (headers != null) {
            for(String hv : headers) {
                if (hv != null && hv.trim().length() > 0) {
                    writer.append(header).append(": ").append(hv).append('\n');
                }
            }
        }
    }


    @Override
    protected String getFilenameExtension() {
        return "txt";
    }

}
