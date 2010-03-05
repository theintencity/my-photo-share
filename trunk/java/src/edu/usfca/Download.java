package edu.usfca;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * The Download service is invoked as GET /api/download/{photo-id} and it returns the
 * photo data. It returns 404 if the photo for this id is not found.
 * It returns 401 if the user is not authenticated or if the user is not allowed
 * to view this file. A user is allowed to view a Photo if he is the author or is someone has
 * shared this Photo with the user.
 * 
 * @author mamta
 */
@SuppressWarnings("serial")
public class Download extends HttpServlet {
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    
    @SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws IOException {
    	
    	String photo = req.getPathInfo().substring(1);
        User u = UserServiceFactory.getUserService().getCurrentUser();
        if (u == null) {
        	System.out.println("download not logged in");
        	res.sendError(401, "Not Authorized");
        	return;
        }
        String email = u.getEmail();
        
        if (!Share.hasAccess(photo, email, false)) {
        	System.out.println("download no access");
        	res.sendError(401, "No Access");
        	return;
        }
        
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	String query = "select from " + Photo.class.getName() + " where photo == '" + photo + "'";
	    	List<Photo> photos = (List<Photo>) pm.newQuery(query).execute();
	    	if (photos.isEmpty()) {
	    		System.out.println("download no photo for " + photo);
	    		res.sendError(404, "Not Found");
	    		return;
	    	}
	    	
    		Photo data = photos.get(0);
    		String key = data.getBlobKey();
    		System.out.println("download photo=" + photo + " user=" + email + " blob-key=" + key);
    		res.setContentType("application/octet-stream");
    		if (key != null) {
		        BlobKey blobKey = new BlobKey(key);
		        blobstoreService.serve(blobKey, res);
    		}
    		else if (data.getData() != null) {
        		byte[] bytes = data.getData().getBytes();
        		System.out.println("returning image of size " + bytes.length);
        		res.setContentLength(bytes.length);
        		res.getOutputStream().write(bytes);
    		}

    	} finally {
    		pm.close();
    	}
    }
}
