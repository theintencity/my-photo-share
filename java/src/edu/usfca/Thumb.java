package edu.usfca;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * This service is invoked as GET /api/thumb/{photo-id} to download the 
 * thumb-nail view of the photo. It returns 401 if the user is not logged in
 * or if the user doesn't have access to view the photo. It returns 404 if the
 * photo id does not exist.
 * 
 * @author mamta
 */
@SuppressWarnings("serial")
public class Thumb extends HttpServlet {
    
    @SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws IOException {
    	
    	String photo = req.getPathInfo().substring(1);
        User u = UserServiceFactory.getUserService().getCurrentUser();
        if (u == null) {
        	System.out.println("thumb not logged in");
        	res.sendError(401, "Not Authorized");
        	return;
        }
        String email = u.getEmail();
        
        if (!Share.hasAccess(photo, email, false)) {
        	System.out.println("thumb no access");
        	res.sendError(401, "No Access");
        	return;
        }
        
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	String query = "select from " + Photo.class.getName() + " where photo == '" + photo + "'";
	    	List<Photo> photos = (List<Photo>) pm.newQuery(query).execute();
	    	if (photos.isEmpty()) {
	    		System.out.println("thumb no photo for " + photo);
	    		res.sendError(404, "Not Found");
	    		return;
	    	}
	    	
    		Photo data = photos.get(0);
    		System.out.println("thumb photo=" + photo + " user=" + email);
    		res.setContentType("application/octet-stream");
    		if (data.getThumb() != null) {
    			byte[] bytes = data.getThumb().getBytes();
    			res.setContentLength(bytes.length);
    			res.getOutputStream().write(bytes);
    		}

    	} finally {
    		pm.close();
    	}
    }
}
