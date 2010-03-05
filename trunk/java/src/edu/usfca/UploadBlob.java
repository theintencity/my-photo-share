package edu.usfca;

import java.io.IOException;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;

/**
 * This service is used to after the client has uploaded an image using the 
 * blobservice API. This service creates the Photo record in the data store and
 * also creates the thumbnail view.
 * 
 * @author mamta
 */
@SuppressWarnings("serial")
public class UploadBlob extends HttpServlet {
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        String user = req.getParameter("user");
    	String photo = req.getPathInfo().substring(1);
    	long size = Long.parseLong(req.getParameter("size"));
    	
        System.out.println("user=" + user + " photo=" + photo);

        Map<String, BlobKey> blobs = blobstoreService.getUploadedBlobs(req);
        BlobKey blobKey = blobs.get("Filedata");
        
        ImagesService imagesService = ImagesServiceFactory.getImagesService();
        Image oldImage = ImagesServiceFactory.makeImageFromBlob(blobKey);

        Transform resize = ImagesServiceFactory.makeResize(256, 256);
        Image newImage = imagesService.applyTransform(resize, oldImage);
        byte[] newImageData = newImage.getImageData();
    	Blob thumb = new Blob(newImageData);
        
        if (blobKey == null) {
        	System.out.println("uploadblob failed to get blob key");
        } else {
            System.out.println("uploadblob photo=" + photo + " user=" + user + " blob-key " + blobKey.getKeyString());
        	Photo data = new Photo();
        	data.setPhoto(photo);
        	data.setAuthor(user);
        	data.setSize(size);
        	data.setBlobKey(blobKey.getKeyString());
        	data.setThumb(thumb);
        	
        	PersistenceManager pm = PMF.get().getPersistenceManager();
        	try {
        		pm.makePersistent(data);
        	}
        	finally {
        		pm.close();
        	}
        }
    }
}
