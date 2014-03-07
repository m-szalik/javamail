javaMail example application
============================

### Run with maven
1. Run it with maven and jetty `mvn jetty:run`
1. Go to <a href="http://localhost:8080/webapp">http://localhost:8080/webapp</a> and click to send an email
1. Use your favourite text editor to see en email in **target/messages** directory

### Configuration:
The only thing you need to do is configure `javax.mail.Session` as `JNDI` resource.
Have a look at [jetty descriptor](./jetty/env.xml) file.

