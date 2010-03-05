<%@ page contentType="text/xml;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="javax.jdo.PersistenceManager" %>
<%@ page import="edu.usfca.Photo" %>
<%@ page import="edu.usfca.PMF" %>
<%@ page import="edu.usfca.Share" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%
/**
 * This service is invokedas GET /api/photos.jsp
 * This service returns list of photo information as follows in XML:
 * <photos>
 *   <photo>
 *     <id>some-photo-id</id>
 *     <caption>photo tags or captions</caption>
 *     <size>numeric-size-in-bytes</size>
 *     <author>email-address-of-author</author>
 *     <dimension>WxH</dimension>
 *   </photo>
 *   ...
 * </photos>
 * 
 * If the user is not authorized, it returns 401 response.
 */

User user = UserServiceFactory.getUserService().getCurrentUser();
if (user == null) {
	response.sendError(401);
	return;
}
else {
	String email = user.getEmail();
%>
<photos>
<%   
	List<Photo> photos = Share.getPhotos(email);
	for (Iterator<Photo> it = photos.iterator(); it.hasNext(); ) {
		Photo photo = it.next();
%>
	<photo>
		<id><%= photo.getPhoto() %></id>
		<caption><%= photo.getCaption() %></caption>
		<size><%= photo.getSize() %></size>
		<author><%= photo.getAuthor() %></author>
		<dimension><%= (photo.getDimension() != null ? photo.getDimension() : "") %></dimension>
	</photo>
<%
	}
%>
</photos>
<%
}
%>
