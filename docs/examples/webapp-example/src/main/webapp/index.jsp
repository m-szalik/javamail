<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css">
        <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap-theme.min.css">
        <title>JavaMail test webapp</title>
        <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
            <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
            <!--[if lt IE 9]>
              <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
              <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
        <![endif]-->
    </head>

    <body>
        <div class="container">
            <div class="header">
                <h1>Example web application for <strong>javaMail extensions</strong> project.</h1>
                <h3>It helps you to develop applications without any external SMTP servers, and you can still get your emails.</h3>
                <p>
                Learn more about it at <a href="https://github.com/m-szalik/javamail/wiki">project's wiki</a>.
                </p>
            </div>

            <hr/>

            <div class="row col-md-6">
                <form method="post" action="<%= request.getContextPath() %>/send" role="form">
                  <div class="form-group">
                    <label for="toEmail1">Email address</label>
                    <input type="email" class="form-control" id="toEmail1" placeholder="Enter email" name="to" value="somebody@somewhere.com">
                  </div>
                  <div class="form-group">
                    <label for="body">Message body</label>
                    <textarea id="body"name="body" class="col-xs-6" style="width:540px; height:200px;">

Dear Visitor,

A few months ago I discovered great javaMail extension.
Visit https://github.com/m-szalik/javamail/wiki for more details.

    Kind Regards
    Developer
                    </textarea>
                  </div>
                  <div class="form-group">
                    <button type="submit" class="btn btn-default">Send</button>
                  </div>
                </form>

                <c:if test="${sent}">
                    <div class="alert alert-success"><strong>Your email has been sent. </strong> You can find it in <i>target/messages</i> directory.</div>
                </c:if>
            </div>
        </div>
    </body>

</html>

