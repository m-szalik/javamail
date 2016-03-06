package org.jsoftware.javamail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jms.BytesMessage;

public class JMS2JavaMailTest {
    private JavaMailSessionDelegate sessionDelegate;
    private JavaMailJMSStatistics javaMailJMSStatistics;
    private JMS2JavaMail jms2JavaMail;

    @Before
    public void setUp() throws Exception {
        sessionDelegate = Mockito.mock(JavaMailSessionDelegate.class);
        javaMailJMSStatistics = Mockito.mock(JavaMailJMSStatistics.class);
        jms2JavaMail = new JMS2JavaMail(sessionDelegate, javaMailJMSStatistics);
    }

    @Test
    @Ignore // TODO
    public void testSuccess() throws Exception {
        BytesMessage message = Mockito.mock(BytesMessage.class);
    }
}