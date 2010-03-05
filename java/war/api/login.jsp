<%@ page contentType="text/xml;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>

<login>
<%
/**
 * The login API is invoked as GET /api/login.jsp?url=... where the url
 * parameter is the client's main page. This service uses Google's user service
 * to create the login URL if user is not logged in or create the logout URL if
 * user is logged in. 
 * 
 * The return format for login success is
 * <login><user>email-address</user><url>logout-url</url></login>
 * 
 * The return format for login failure is
 * <login><url>login-url</url></login>
 * 
 * The client uses the login-url or logout-url accordingly to redirect the user
 * to login page or to present the signout button.
 */
UserService userService = UserServiceFactory.getUserService();
User user = userService.getCurrentUser();
if (user != null) {
%>
  <user><%= user.getEmail() %></user>
  <url><%= userService.createLogoutURL(request.getParameter("url")) %></url>
<%
} else {
%>
  <url><%= userService.createLoginURL(request.getParameter("url")) %></url>
<%
}
%>
</login>
