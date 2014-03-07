javaMail extensions
===================

### For developers:
Allows sending emails form your application using java.mail.Session to a file that is helpful during development phase.


### For admins:
Allows also sending emails using java.mail.Session to JMS queue and than via SMTP. This solutions seeds up sending process because it doesn't require to connect to real smtp server.
It can be deployed on application server (as default mailSession) and it's completely transparent for applications.
You can choose transport one of:
* filemsg = emails are saved to files in mbox format
* filetxt = emails are saved to files in text format (only headers and text/plain part of message)
* nop     = no file is created, only info in logs

#### Project modules:
* javamail-file-transport = javaMail transport that stores emails in files - for developers
* javamail-jms-transport = javaMail transport that sends email to JMS queue - for admins
* javamail-jms2javamail = an EJB that copies emails from JMS to real javaMail session - for admins
* javamail-test-webapp = testing/example web application

#### Configuration
For configuration examples see [docs/README.txt](./docs/README.txt)

#### Demo 
It takes **only 5 steps** to see how it works.
You can download a webapp application, wich works with jetty to see how it works.
1. Clone project `git clone https://github.com/m-szalik/javamail.git`
1. Enter javamail-test-webapp directory `cd javamail/javamail-test-webapp/`
1. Run it with maven and jetty `mvn jetty:run`
1. Go to [http://localhost:8080/webapp](http://localhost:8080/webapp) and click to send an email
1. Use your favourite text editor to see en email in **target/messages** directory

### License
Apache License 2.0

### Problems and questions
In case of problems or questions contact me by [creating an issue](https://github.com/m-szalik/javamail/issues/new) on GitHub.

### More info on Wiki
https://github.com/m-szalik/javamail/wiki
