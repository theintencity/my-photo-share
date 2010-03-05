package edu.usfca;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * This class implements the delete feature of the photo sharing application.
 * This is invoked by GET /api/delete/{photo-id} to delete the phone with id.
 * The user must be author of this Photo.
 * It returns response of 404 if id not found, or 401 if user is not logged in
 * or user is not the author.
 * 
 * @author mamta
 *
 */
@SuppressWarnings("serial")
public class Delete extends HttpServlet {
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    
    @SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    	
    	String photo = req.getPathInfo().substring(1);
        User u = UserServiceFactory.getUserService().getCurrentUser();
        if (u == null) {
        	System.out.println("delete not logged in");
        	res.sendError(401, "Not Authorized");
        	return;
        }
        String email = u.getEmail();
        
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	String query = "select from " + Photo.class.getName() + " where photo == '" + photo + "'";
	    	List<Photo> photos = (List<Photo>) pm.newQuery(query).execute();
	    	if (photos.isEmpty()) {
	    		System.out.println("delete no photo for " + photo);
	    		res.sendError(404, "Not Found");
	    		return;
	    	}
	    	
    		Photo data = photos.get(0);
    		if (!email.equals(data.getAuthor())) {
    			System.out.println("invalid email " + email + " " + data.getAuthor());
    			res.sendError(401, "Not Authorized");
    			return;
    		}
    		
    		System.out.println("delete photo=" + photo + " user=" + email);
    		
    		if (data.getBlobKey() != null) {
    			System.out.println("delete blob-key " + data.getBlobKey());
    			blobstoreService.delete(new BlobKey(data.getBlobKey()));
    		}
    		
    		pm.deletePersistent(data);
    	} finally {
    		pm.close();
    	}
    }
}
