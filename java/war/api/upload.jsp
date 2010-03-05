<%@ page contentType="text/xml;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>

<%
/**
 * This service is invoked as GET /api/upload.jsp?&photo=...&email=...&size=...
 * The client supplies the photo id, email address of the owner and the size in bytes of this
 * photo file. If the size is more than 1 MB, this service returns
 * <upload><url>url-to-upload-file-using-blobservice</url></upload>
 * otherwise this service returns
 * <upload><url>/api/uploadfile/{photo-id}?user=...&size=...</url></upload>.
 * 
 * The actual upload is done in UploadBlob.java or UploadFile.java.
 */

BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
UserService userService = UserServiceFactory.getUserService();
User user = userService.getCurrentUser();
long size = Long.parseLong(request.getParameter("size"));
if (size >= 1000000) {
%>
<upload>
	<url><%= blobstoreService.createUploadUrl("/api/uploadblob/" + request.getParameter("photo") + "?user=" + user.getEmail() + "&size=" + size ) %></url>
</upload>
<%
} else {
%>
<upload>
	<url>/api/uploadfile/<%= request.getParameter("photo") + "?user=" + user.getEmail() + "&size=" + size  %></url>
</upload>
<%
}
%>