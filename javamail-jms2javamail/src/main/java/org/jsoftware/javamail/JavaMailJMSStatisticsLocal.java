package org.jsoftware.javamail;

import javax.ejb.Local;
import javax.mail.Address;
import javax.mail.internet.MimeMessage;

/**
 * @author szalik
 */
@Local
public interface JavaMailJMSStatisticsLocal {

    void onSuccess(MimeMessage mimeMessage, Address[] addresses);

    void onFailure(MimeMessage mimeMessage, Address[] addresses, Exception ex);

}
