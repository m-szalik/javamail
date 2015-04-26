package org.jsoftware.javamail;

import javax.management.openmbean.CompositeData;
import java.util.Date;

/**
 * JavaMail statistics MXBean interface
 * @author szalik
 */
public interface JavaMailJMSStatisticsMXBean {

    /**
     * @return when we started to collect statistics
     */
    Date getStatisticsCollectionStartDate();

    /**
     * @return Info about last email sent
     */
    CompositeData getLastSentMailInfo();

    /**
     * @return Info about last email that failed to send
     */
    CompositeData getLastFailureMailInfo();

    /**
     * @return how many emails has been sent since getStatisticsCollectionStartDate
     * @see #getStatisticsCollectionStartDate()
     */
    long getEmailsSentSuccessCounter();


    /**
     * @return how many emails hasn't been sent since getStatisticsCollectionStartDate
     * @see #getStatisticsCollectionStartDate()
     */
    long getEmailsSentFailureCounter();

    /**
     * Reset stats
     */
    void reset();
}
