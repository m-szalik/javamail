
## How it works:

    Application ---> mail/session(with file transport as default) ---> Mail saved as file in local directory.

![Text transport diagram](https://raw.githubusercontent.com/m-szalik/javamail/master/docs/javamail-introduction-dev.png)

How to install JavaMail File Transport:
1) Add javamail-file-transport.jar into calsspath.
2) Set properties for Javamail Session as follows:
 * **mail.files.path** = location of emails directory
 * **mail.transport.protocol** = default transport for Javamail Sesion
   * **smtp** = this is JavaMail default
   * **nop** = NoOperation, no email will be send
   * **filemsg** = Send into a file in mbox format
   * **filetext** = Send into a file in plain text format

## Configuration examples:
 * [Jetty configuration](../docs/examples/webapp-example/jetty/env.xml)
 * [Standalone app - MainAppExample.java](../docs/examples/standalone-example/src/org/jsoftware/javamail/MainAppExample.java)





