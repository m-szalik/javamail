Versions:
	1.4.4 for jdk6
	
How it works:
    Application ---> JMSTransport ---> JMS mailQueue
    JMS mailQueue ---> XXXTransport


How to install JavaMail JMS Support:
1) Copy JavaMail JMS Transport jar into server libs directory. (eg. glassfish3/glassfish/lib)
2) Create logger cofiguration for "org.jsoftware.javamail.*" OPTIONAL
3) Create JNDI resources:
    a) JNDI: jms/queueConnectionFactory		Type: javax.jms.QueueConnectionFactory
    b) JNDI: jms/mailQueue			Type: javax.jms.Queue			(this is where messages will be stored)
    c) JNDI: mail/session			Type: javax.mail.Sessios
	Configure two transports:
	    i) JMS Transport (as default) 
		Protocol: smtpjms
		Transport Protocol Class: org.jsoftware.javamail.SmtpJmsTransport
		Configuration properties:
		    property name				default value			description
		    mail.smtpjms.jmsQueueConnectionFactory	jms/queueConnectionFactory	JMS connection.
		    mail.smtpjms.jmsQueue			jms/mailQueue			JMS Queue.			(DO NOT CHANGE IT)
		    mail.smtpjms.validateFrom			true				Check if mail field FROM is set.
		    mail.smtpjms.dstProtocol			smtp				Destination javaMail protocol. Used for real messages sending.
Smtp options:
            mail.smtp.host                          localhost                   Host for SMTP transport
           	mail.smtp.port                          25                          Port for SMTP transport
            mail.smtp.auth                          false                       Auth for SMTP transport
            mail.smtp.starttls.enable               -                           Auth option for SMTP transport

	    ii) SMTP or other (this will be used for real messages sending)
		Protocol: (see configuration for JMS TRansport)		default: smtp
		Transport Protocol Class:				default: com.sun.mail.smtp.SMTPTransport
		Other configuration properties required by the transport.
4) Deploy Message Driven Bean that transfers messages from JMSQueue (jms/mailQueue) to "dstProtocol" of javaMail Session.
5) Use mail/session JNDI resource in your applications. Do not forget to setup FROM field in each message.


You can set some special message headers:
"X-Send-priority"	sending priorioty witch is jms message priority (0-9) or "low"=1 or "high"=8
"X-Send-expire"		sending timeout in ms.






