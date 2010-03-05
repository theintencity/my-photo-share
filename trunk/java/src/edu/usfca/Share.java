package edu.usfca;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * The service is invoked as either
 * GET /api/share?id=id1,id2   or
 * GET /api/share?id=id2,id2&add=email2,email2&remove=email3,email4
 * 
 * In the first option it returns an XML response with the list of user emails
 * with whom the supplied photo-ids are shared with.
 * <share>
 *   <user>email1</user>
 *   <user>email2</user>
 * </share>
 * 
 * In the second option it adds and removed the supplied email addresses to which
 * all the supplied photo-ids are shared with.
 * 
 * The logged in user must have access to all the the photo-ids, otherwise it returns 401.
 * 
 * @author mamta
 */
@SuppressWarnings("serial")
public class Share extends HttpServlet {
    
    public void doPost(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    	doGet(req, res);
    }
    
    @SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    	
        User u = UserServiceFactory.getUserService().getCurrentUser();
        if (u == null) {
        	System.out.println("update not logged in");
        	res.sendError(401, "Not Authorized");
        	return;
        }
        String email = u.getEmail();
        
        if (req.getParameter("id") == null || req.getParameter("id").length() == 0) {
        	res.sendError(400, "Must supply list of photo id");
        	return;
        }
        
        String[] photos = req.getParameter("id").split(",");
        String adds = req.getParameter("add");
        String removes = req.getParameter("remove");
        
        for (int i=0; i<photos.length; ++i) {
        	if (!Share.hasAccess(photos[i], email, false)) {
        		res.sendError(401, "Not author of some or all files");
        		return;
        	}
        }
        
        PrintWriter out = res.getWriter();
        
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
    		if (adds == null && removes == null) {
    			// retrieve share
    			Set<String> allowed = new HashSet<String>();
    			
    			res.setContentType("text/xml");
    			
    			System.out.println("share id=" + req.getParameter("id"));
    			for (int i=0; i<photos.length; ++i) {
    				String photo = photos[i];
        	    	String query = "select from " + ShareData.class.getName() + " where photo == '" + photo + "' && owner == '" + email + "'";
        	    	List<ShareData> result = (List<ShareData>) pm.newQuery(query).execute();
        	    	for (Iterator<ShareData> it=result.iterator(); it.hasNext(); ) {
        	    		ShareData shareData = it.next();
        	    		System.out.println("share photo=" + photo + " user=" + shareData.getUser());        	    		allowed.add(shareData.getUser());
        	    	}
    			}
    			out.print("<share>\n");
    			for (Iterator<String> it = allowed.iterator(); it.hasNext(); ) {
        			out.print("  <user>" + it.next() + "</user>\n");
    			}
    	    	out.print("</share>\n");
    		}
    		else {
    			// set share
    			System.out.println("share id=" + req.getParameter("id") + " add=" + adds + " remove=" + removes);
    			
    			String[] addUsers = (adds != null ? adds.split(",") : null);
    			String[] removeUsers = (removes != null ? removes.split(",") : null);
    			List<ShareData> toDelete = new LinkedList<ShareData>();
    			List<ShareData> toAdd = new LinkedList<ShareData>();
    			
    			for (int i=0; i<photos.length; ++i) {
    				String photo = photos[i];
    				for (int j=0; removeUsers != null && j<removeUsers.length; ++j) {
    					String user = removeUsers[j];
    					if (user == null || user.length() == 0)
    						continue;
            	    	String query = "select from " + ShareData.class.getName() + " where photo == '" + photo + "' && owner == '" + email + "' && user == '" + user + "'";
            	    	List<ShareData> result = (List<ShareData>) pm.newQuery(query).execute();
            	    	if (!result.isEmpty()) {
            	    		System.out.println("removing photo=" + photo + ", user=" + user + ", owner=" + email);
            	    		toDelete.addAll(result);
            	    	}
    				}
    				for (int j=0; addUsers != null && j<addUsers.length; ++j) {
    					String user = addUsers[j];
    					if (user == null || user.length() == 0)
    						continue;
            	    	String query = "select from " + ShareData.class.getName() + " where photo == '" + photo + "' && owner == '" + email + "' && user == '" + user + "'";
            	    	List<ShareData> result = (List<ShareData>) pm.newQuery(query).execute();
            	    	if (result.isEmpty()) {
            	    		System.out.println("adding photo=" + photo + ", user=" + user + ", owner=" + email);
            	    		ShareData shareData = new ShareData();
            	    		shareData.setOwner(email);
            	    		shareData.setUser(user);
            	    		shareData.setPhoto(photo);
            	    		toAdd.add(shareData);
            	    	}
    				}
    			}
    			
	    		pm.deletePersistentAll(toDelete);
	    		pm.makePersistentAll(toAdd);
    		}
    	} finally {
    		pm.close();
    	}
    }
    
    /**
     * The class method is used to get the list of available photos for the given user.
     * It first selects all the photos owned by the user (first list).
     * Then it selects all the photos shared by others to this user (second list). 
	 * If the user has some privacy list, then it filters the second list above with
	 * the list of email addresses in this privacy list, otherwise it uses the
	 * second list as a whole.
	 * 
     * @param user
     * @return list of photo objects viewable by the supplied user.
     */
    @SuppressWarnings("unchecked")
	public static List<Photo> getPhotos(String user) {
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	List<Photo> result = new LinkedList<Photo>();
	    	String query = "select from " + Photo.class.getName() + " where author == '" + user + "'";
	    	List<Photo> result1 = (List<Photo>) pm.newQuery(query).execute();
	    	result.addAll(result1);
	    	
	    	query = "select from " + PrivacyData.class.getName() + " where author == '" + user + "'";
	    	List<PrivacyData> p = (List<PrivacyData>) pm.newQuery(query).execute();
	    	boolean allowAll = true;
	    	Set<String> privacy = new HashSet<String>();
    		for (Iterator<PrivacyData> it=p.iterator(); it.hasNext(); ) {
    			PrivacyData privacyData = it.next();
    			System.out.println("privacyData author=" + privacyData.getAuthor() + " allow=" + privacyData.getAllow());
    			String allow = privacyData.getAllow();
    			if (allow != null && allow.length() > 0) {
    				privacy.add(privacyData.getAllow());
    				allowAll = false;
    			}
    			else {
    				pm.deletePersistent(privacyData);
    			}
    		}
	    	
	    	query = "select from " + ShareData.class.getName() + " where user == '" + user + "'";
	    	List<ShareData> shared = (List<ShareData>) pm.newQuery(query).execute();
	    	for (Iterator<ShareData> it=shared.iterator(); it.hasNext(); ) {
	    		ShareData shareData = it.next();
		    	String query2 = "select from " + Photo.class.getName() + " where photo == '" + shareData.getPhoto() + "'";
		    	List<Photo> result2 = (List<Photo>) pm.newQuery(query2).execute();
		    	if (allowAll || !result2.isEmpty() && privacy.contains(result2.get(0).getAuthor()))
		    		result.addAll(result2);
		    	else
		    		System.out.println("file not allowed " + (result2.isEmpty() ? "null" : result2.get(0).getPhoto()));
	    	}
	    	
	    	return result;
    	} finally {
    		pm.close();
    	}
    }
    
    /**
     * Check whether the given photo is allowed to be viewed by user. 
     * If the mustOwn argument is true then the user must be owner of the photo
     * to return true, otherwise it returns true if the user is either the 
     * owner of the photo or someone shared the photo with the user.
     * 
     * @param photo
     * @param user
     * @param mustOwn
     * @return
     */
    @SuppressWarnings("unchecked")
	public static boolean hasAccess(String photo, String user, boolean mustOwn) {
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
    		// check whether user owns photo
	    	String query = "select from " + Photo.class.getName() + " where photo == '" + photo + "'";
	    	List<Photo> result = (List<Photo>) pm.newQuery(query).execute();
	    	if (!result.isEmpty() && user.equals(result.get(0).getAuthor())) {
	    		return true;
	    	}
	    	else if (mustOwn) {
	    		System.out.println("Share.hasAccess() " + user + " does not own " + photo);
	    		return false;
	    	}
	    	
	    	// of else if someone has shared photo with user?
	    	String query2 = "select from " + ShareData.class.getName() + " where photo == '" + photo + "' && user == '" + user + "'";
	    	List<ShareData> result2 = (List<ShareData>) pm.newQuery(query2).execute();
	    	if (!result2.isEmpty()) {
	    		return true;
	    	}
	    	else {
	    		System.out.println("Share.hasAccess() " + user + " cannot access " + photo);
	    		return false;
	    	}
    	}
    	finally {
    		pm.close();
    	}
    }
        
}
