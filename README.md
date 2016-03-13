javaMail extensions
===================

[![Join the chat at https://gitter.im/m-szalik/javamail](https://badges.gitter.im/m-szalik/javamail.svg)](https://gitter.im/m-szalik/javamail?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/m-szalik/javamail.svg?branch=master)](https://travis-ci.org/m-szalik/javamail)
[![codecov.io](https://codecov.io/github/m-szalik/javamail/coverage.svg?branch=master)](https://codecov.io/github/m-szalik/javamail?branch=master)
[![Dependency Status](https://www.versioneye.com/user/projects/56e2c695df573d00431139b0/badge.svg?style=flat)](https://www.versioneye.com/user/projects/56e2c695df573d00431139b0)

### More info on Wiki
https://github.com/m-szalik/javamail/wiki

### For developers:
Allows sending emails form your application using java.mail.Session to a file that is helpful during development phase.
![file transport](https://raw.githubusercontent.com/m-szalik/javamail/master/docs/javamail-introduction-dev.png "File transport")


### For admins:
Allows also sending emails using java.mail.Session to JMS queue and than via SMTP. This solutions seeds up sending process because it doesn't require to connect to real smtp server.
It can be deployed on application server (as default mailSession) and it's completely transparent for applications.
You can choose transport one of:
* **filemsg** = emails are saved to files in mbox format
* **filetxt** = emails are saved to files in text format (only headers and text/plain part of message)
* **nop**     = no file is created, only info in logs

![jms transport](https://raw.githubusercontent.com/m-szalik/javamail/master/docs/javamail-introduction-admin.png "JMS transport")

#### Project modules:
* **javamail-file-transport** = javaMail transport that stores emails in files - for developers
* **javamail-jms-transport** = javaMail transport that sends email to JMS queue - for admins
* **javamail-jms2javamail** = an EJB that copies emails from JMS to real javaMail session - for admins

#### Configuration
For configuration examples see [docs/README.md](./docs/README.md)

#### Webapp demo (Jetty)
It takes **5 steps only** to see how it works.
You can download a webapp application, which works with jetty to see how it works.
 1. Clone project `git clone https://github.com/m-szalik/javamail.git`
 1. Enter javamail-test-webapp directory `cd javamail/docs/examples/webapp-example/`
 1. Run it with maven and jetty `mvn jetty:run`
 1. Go to [http://localhost:8080/webapp](http://localhost:8080/webapp) and click to send an email
 1. Use your favourite text editor to view an email located in **target/messages** directory

### License
Apache License 2.0

### Problems and questions
In case of problems or questions contact me by [creating an issue](https://github.com/m-szalik/javamail/issues/new) on GitHub or via [![Join the chat at https://gitter.im/m-szalik/javamail](https://badges.gitter.im/m-szalik/javamail.svg)](https://gitter.im/m-szalik/javamail?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge).


