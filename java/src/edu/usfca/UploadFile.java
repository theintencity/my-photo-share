package edu.usfca;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;

/**
 * This service is used to upload a file less than 1 MB size to the data store.
 * The client uses FileReference.upload() method to upload the file.
 * The client also supplied the size and user parameters in the URL.
 * It creates the Photo record in the data store and also creates a thumb-nail
 * view of the image is 256x256 dimension.
 * 
 * @author mamta
 */
@SuppressWarnings("serial")
public class UploadFile extends HttpServlet {
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        String user = req.getParameter("user");
    	String photo = req.getPathInfo().substring(1);
    	long size = Long.parseLong(req.getParameter("size"));
    	
        System.out.println("user=" + user + " photo=" + photo + " size=" + size);
		Blob imageData = null;
		
		ServletFileUpload upload = new ServletFileUpload();
		upload.setFileSizeMax(5000000);
		upload.setSizeMax(-1);
		
		try {
		    FileItemIterator it = upload.getItemIterator(req);
		    while (it.hasNext()) {
                FileItemStream item = it.next();
                if (!item.isFormField()) {
                	InputStream input = item.openStream();
                	
                	System.out.println("available " + input.available());
            		byte[] bytes = new byte[(int) size];
            		int i=0;
                	int c;
                	while ((c = input.read()) != -1) {
                		bytes[i++] = (byte) c;
                	}
                	System.out.println("written " + i + " bytes");
                	imageData = new Blob(bytes);
	            }
		    }
		} catch (FileUploadException e) {
	          e.printStackTrace();
	          res.sendError(500, "Exception in File Upload");
	          return;
		}
      
		if (imageData == null) {
			res.sendError(400, "No Image Data");
			return;
		}
		
        ImagesService imagesService = ImagesServiceFactory.getImagesService();
        Image oldImage = ImagesServiceFactory.makeImage(imageData.getBytes());
        String dimension = String.valueOf(oldImage.getWidth()) + "x" + String.valueOf(oldImage.getHeight());
        
        Transform resize = ImagesServiceFactory.makeResize(256, 256);
        Image newImage = imagesService.applyTransform(resize, oldImage, ImagesService.OutputEncoding.JPEG);
        byte[] newImageData = newImage.getImageData();
    	Blob thumb = new Blob(newImageData);

    	Photo data = new Photo();
    	data.setPhoto(photo);
    	data.setAuthor(user);
    	data.setSize(size);
    	data.setData(imageData);
    	data.setThumb(thumb);
    	data.setDimension(dimension);
    	System.out.println("file dimension=" + dimension + " size=" + size);
    	
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
    		pm.makePersistent(data);
    	}
    	finally {
    		pm.close();
    	}
	    System.out.println("returning success");
    }
}
