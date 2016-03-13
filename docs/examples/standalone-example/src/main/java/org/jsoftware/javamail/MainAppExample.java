package org.jsoftware.javamail;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Properties;

/**
 * Example
 */
public class MainAppExample {

    public static void main(String[] args) throws MessagingException {
        Properties javaMailConfiguration = new Properties();
        javaMailConfiguration.put("mail.transport.protocol", "filetxt");
        javaMailConfiguration.put("mail.files.path", "target/messages");
        Session session = Session.getDefaultInstance(javaMailConfiguration);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("from@server.com"));
        InternetAddress[] address = { new InternetAddress("to@server.com") };
        message.setRecipients(Message.RecipientType.TO, address);
        Date now = new Date();
        message.setSubject("JavaMail test at " + now);
        message.setSentDate(now);
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Body text version", "UTF-8");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<p>Body's text <strong>(html)</strong></p><p>Body html version</p>", "text/html; charset=UTF-8");
        Multipart multiPart = new MimeMultipart("alternative");
        multiPart.addBodyPart(textPart);
        multiPart.addBodyPart(htmlPart);
        message.setContent(multiPart);
        session.getTransport().sendMessage(message, address);
    }

}
