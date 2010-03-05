package edu.usfca;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * This service is invoked as
 *  GET /api/update/{photo-id}?caption=new-caption-or-tags
 *  GET /api/update/{photo-id}?rotate=90 or rotate=270
 *  GET /api/update/{photo-id}?flip=horizontal or flip=vertical
 *  GET /api/update/{photo-id}?resize=WxH
 *  
 * The service modifies the image data or metadata using the supplied arguments.
 * It returns 404 if the photo-id is not valid. It returns 401 if the logged in
 * user is not the author of the photo.
 * 
 * It uses the Google image service to transform the image for rotate, flip or resize.
 * After successful update of the image data it recreates the thumb-nail
 * view of the image in 256x256 size.
 * 
 * @author mamta
 */
@SuppressWarnings("serial")
public class Update extends HttpServlet {
    
    public void doPost(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    	doGet(req, res);
    }
    
    @SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    	
    	String photo = req.getPathInfo().substring(1);
        User u = UserServiceFactory.getUserService().getCurrentUser();
        if (u == null) {
        	System.out.println("update not logged in");
        	res.sendError(401, "Not Authorized");
        	return;
        }
        String email = u.getEmail();
        String caption = req.getParameter("caption");
        String rotate = req.getParameter("rotate");
        String flip = req.getParameter("flip");
        String resize= req.getParameter("resize");
        
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	String query = "select from " + Photo.class.getName() + " where photo == '" + photo + "'";
	    	List<Photo> photos = (List<Photo>) pm.newQuery(query).execute();
	    	if (photos.isEmpty()) {
	    		System.out.println("update no photo for " + photo);
	    		res.sendError(404, "Not Found");
	    		return;
	    	}
	    	
    		Photo data = photos.get(0);
    		if (!email.equals(data.getAuthor())) {
    			System.out.println("invalid email " + email + " " + data.getAuthor());
    			res.sendError(401, "Not Authorized");
    			return;
    		}
    		
    		System.out.println("update photo=" + photo + " user=" + email + " size=" + data.getData().getBytes().length);
    		
    		if (caption != null) {
    			System.out.println("updating caption from \"" + data.getCaption() + "\" to \"" + caption + "\"");
    			data.setCaption(caption);
    		}
    		
    		if (rotate != null || flip != null || resize != null) {
    			if (data.getData() == null) {
    				System.out.println("cannot update image on size more than 1 MB");
    				res.sendError(500, "Cannot update image on size more than 1 MB");
    				return;
    			}
    	        ImagesService imagesService = ImagesServiceFactory.getImagesService();
				Image oldImage = ImagesServiceFactory.makeImage(data.getData().getBytes());
    	        
	    		Transform transform;
	    		
	    		if (flip != null) {
	    			System.out.println("updating flip " + flip);
	    			if (flip.equals("vertical"))
	    				transform = ImagesServiceFactory.makeVerticalFlip();
	    			else if (flip.equals("horizontal"))
	    				transform = ImagesServiceFactory.makeHorizontalFlip();
	    			else {
	    				res.sendError(400, "Invalid flip value " + flip);
	    				return;
	    			}
	    		}
	    		else if (rotate != null) {
	    			System.out.println("updating rotate " + rotate);
	    			int degree = Integer.parseInt(rotate);
	    			transform = ImagesServiceFactory.makeRotate(degree);
	    		}
	    		else if (resize != null) {
	    			System.out.println("updating resize " + resize);
	    			String[] parts = resize.split("x");
	    			if (parts.length != 2) {
	    				res.sendError(400, "Invalid resize format - user WxH");
	    				return;
	    			}
	    			int width = Integer.parseInt(parts[0]);
	    			int height = Integer.parseInt(parts[1]);
	    			if (width <= 0 || height <= 0) {
	    				res.sendError(400, "Invalid resize size must be positive integer");
	    				return;
	    			}
	    			
	    	        transform = ImagesServiceFactory.makeResize(width, height);
	    		}
	    		else {
	    			res.sendError(400, "Invalid resize, rotate or flip");
	    			return;
	    		}
	    		
    	        Image newImage = imagesService.applyTransform(transform, oldImage, ImagesService.OutputEncoding.JPEG);
    	        System.out.println("transform size=" + oldImage.getImageData().length + " to " + newImage.getImageData().length);
    	        data.setData(new Blob(newImage.getImageData()));
    	        
    	        Transform thumb = ImagesServiceFactory.makeResize(256, 256);
    	        Image thumbImage = imagesService.applyTransform(thumb, newImage, ImagesService.OutputEncoding.JPEG);
    	    	data.setThumb(new Blob(thumbImage.getImageData()));
    		}
    		
    	} finally {
    		pm.close();
    	}
    }
}
