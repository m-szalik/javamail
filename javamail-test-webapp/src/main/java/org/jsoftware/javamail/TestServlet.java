package org.jsoftware.javamail;


import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Example servlet for sending emails via javax.mail.Session.
 */
public class TestServlet extends HttpServlet {

    /** On Tomcat application server - @Resource loads global mailSession but should load those defined in context.xml */
	private transient Session mailSession;


    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse response) throws ServletException, IOException {
        httpServletRequest.getRequestDispatcher("/index.jsp").forward(httpServletRequest, response);
    }


    /**
     * Sand example email
     */
    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse response) throws ServletException, IOException {
        String toEmail = httpServletRequest.getParameter("to");
        String body = httpServletRequest.getParameter("body");
        if (toEmail == null || toEmail.length() == 0) {
            throw new ServletException("No \"to\" parameter!");
        }
        if (body == null || body.length() == 0) {
            body = "No body";
        }

        try {
            Date now = new Date();
            String from = mailSession.getProperty("mail.from");
            if (from == null || from.length() == 0) {
                from = "test@test.com";
            }
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));
            InternetAddress[] address = { new InternetAddress(toEmail) };
            message.setRecipients(Message.RecipientType.TO, address);
            message.setSubject("JavaMail test at " + now);
            message.setSentDate(now);
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Body's text (text)", "UTF-8");
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent("<p>Body's text <strong>(html)</strong></p>", "text/html; charset=UTF-8");
            Multipart multiPart = new MimeMultipart("alternative");
            multiPart.addBodyPart(textPart);
            multiPart.addBodyPart(htmlPart);
            message.setContent(multiPart);
            mailSession.getTransport().sendMessage(message, address);
            httpServletRequest.setAttribute("sent", Boolean.TRUE);
        } catch (Exception ex) {
            httpServletRequest.setAttribute("sent", Boolean.FALSE);
            throw new ServletException("Error sending example e-mail!", ex);
        }
        doGet(httpServletRequest, response);
    }


    /**
     * Get Session from JNDI
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            mailSession = (Session) envCtx.lookup("mail/Session");
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
}
