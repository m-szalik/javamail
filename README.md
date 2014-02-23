javaMail extensions
===================

### For developers:
Allows sending emails form your application using java.mail.Session to a file that is helpful during development phase.


### For admins:
Allows also sending emails using java.mail.Session to JMS queue and than via SMTP. This solutions seeds up sending process because it doesn't require to connect to real smtp server.
It can be deployed on application server (as default mailSession) and it's completely transparent for applications.

#### Project modules:
* javamail-file-transport = javaMail transport that stores emails in files - for developers
* javamail-jms-transport = javaMail transport that sends email to JMS queue - for admins
* javamail-jms2javamail = an EJB that copies emails from JMS to real javaMail session - for admins
* javamail-test-webapp = testing/example web application

#### Configuration
For configuration examples see [docs/README.txt](./docs/README.txt)

### License
Apache License 2.0
