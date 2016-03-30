package org.jsoftware.javamail;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author szalik
 */
public class AbstractFileTransportTest {
    private final static String BASE_NAME = "base-name";
    private final static String BASE_EXT = "ext";
    private MimeMessage message;
    private AbstractFileTransport transport;

    @Before
    public void setup() throws MessagingException, IOException {
        Properties properties = new Properties();
        properties.put("mail.files.path", "target" + File.separatorChar + "output");
        Session session = Session.getDefaultInstance(properties);
        message = new MimeMessage(session);
        message.setFrom("Test <test@jsoftware.org>");
        transport = new AbstractFileTransport(session, new URLName("AbstractFileDev")) {
            @Override
            protected void writeMessage(Message message, OutputStream os) throws IOException, MessagingException {
            }
            @Override
            protected String getFilenameExtension() {
                return BASE_EXT;
            }
        };
        cleanup();
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    private void cleanup() {
        File[] files = transport.getDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(BASE_NAME) && name.endsWith("." + BASE_EXT);
            }
        });
        if (files != null) {
            for(File f : files) {
                f.delete();
            }
        }
    }

    @Test
    public void testFilename() throws Exception {
        File f0 = transport.createMessageFile(BASE_NAME);
        File f1 = transport.createMessageFile(BASE_NAME);
        Assert.assertEquals("base-name.ext", f0.getName());
        Assert.assertEquals("base-name-1.ext", f1.getName());
    }
}
