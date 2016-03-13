## Version info:
1.5.5 for jdk7 and newer
	
## How it works:

    Application ---> mail/session(JMSTransport as default) ---> JMS mailQueue
    JMS mailQueue ---> XXXTransport(specified by "mail.smtpjms.dstProtocol" property)


## How to install JavaMail JMS Support:
1) Copy JavaMail JMS Transport jar into server libs directory. (eg. glassfish3/glassfish/lib or glassfish3/glassfish/lib/endorsed)
2) It is likely that you need to copy also javax.mail-1.5.2.jar - https://maven.java.net/content/repositories/releases/com/sun/mail/javax.mail/1.5.2/
3) OPTIONAL: Create logger configuration for "org.jsoftware.javamail.*" (this software uses java.util.logging as logging provider)
4) Create JNDI resources:
 * JNDI: jms/queueConnectionFactory		Type: javax.jms.QueueConnectionFactory
 * JNDI: jms/mailQueue			Type: javax.jms.Queue			(this is where messages will be stored)
 * JNDI: mail/Session			Type: javax.mail.Session        (only one is required)
 * Configure two transports on single instance of mail/Session:
    * 1st transport - JMS Transport (as default) - see below
    * 2nd transport - SMTP Transport (used by javamail-jms2javamail EJB to send emails) - see below
5) Deploy Message Driven Bean (project:javamail-jms2javamail) that transfers messages from JMSQueue (jms/mailQueue) to "dstProtocol" of javaMail Session.
6) Use mail/session JNDI resource in your applications. Do not forget to setup FROM field in each message.


### JMS Transport configuration:

 * Protocol: **smtpjms**
 * Transport Protocol Class: *org.jsoftware.javamail.SmtpJmsTransport*
 * Configuration properties:

| property name | default value | description |
| ------------- |:-------------:| -----:|
| mail.smtpjms.jmsQueueConnectionFactory | jms/queueConnectionFactory | JMS connection |
| mail.smtpjms.jmsQueue | jms/mailQueue	| JMS Queue. (DO NOT CHANGE IT) |
| mail.smtpjms.validateFrom | true | Check if mail field FROM is set |
| mail.smtpjms.dstProtocol | smtp | Destination javaMail protocol. Used for real messages sending |


Smtp options:

| property name | default value | description |
| ------------- |:-------------:| -----:|
| mail.smtp.host | localhost | Host for SMTP transport |
| mail.smtp.port | 25 | Port for SMTP transport |
| mail.smtp.auth | false | Auth for SMTP transport |
| mail.smtp.starttls.enable | false | Auth option for SMTP transport |

### SMTP Transport configuration:
_SMTP or other (this will be used for real messages sending)_

 * Protocol: (see configuration for JMS Transport)		default: smtp
 * Transport Protocol Class:				            default: com.sun.mail.smtp.SMTPTransport
 * Other configuration properties required by the transport.

# Extra mail features:
You can set some special message headers:
"X-Send-priority"	sending priority witch is jms message priority (0-9) or "low"=1 or "high"=8
"X-Send-expire"		sending timeout in ms.






