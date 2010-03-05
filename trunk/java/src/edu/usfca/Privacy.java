package edu.usfca;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * This service is invoked GET /api/privacy or GET /api/privacy?add=email1,email2,...
 * For the first method, it returns the list of users from whom the logged in user
 * want to view photos from as XML response:
 * <privacy>
 *   <user>email1</user>
 *   <user>email2</user>
 * </privacy>
 * 
 * In the second method, it replaces the existing list of users with the supplied
 * list of user emails in the privacy option. All the existing user emails which
 * do not exist in the supplied list are removed, and all the new emails which
 * do not exist in the existing list are added. 
 * 
 * @author mamta
 *
 */
@SuppressWarnings("serial")
public class Privacy extends HttpServlet {
    
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
        String users = req.getParameter("add");
        
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
    		if (users == null) {
    			// retrieve privacy
    			res.setContentType("text/xml");
    			
                PrintWriter out = res.getWriter();
    			out.print("<privacy>\n");
    			
    	    	String query = "select from " + PrivacyData.class.getName() + " where author == '" + email + "'";
    	    	List<PrivacyData> result = (List<PrivacyData>) pm.newQuery(query).execute();
    	    	for (Iterator<PrivacyData> it=result.iterator(); it.hasNext(); ) {
    	    		PrivacyData data = it.next();
    	    		out.print("  <user>" + data.getAllow() + "</user>\n");
    	    	}
    	    	out.print("</privacy>\n");
    		}
    		else {
    			// set privacy
    			
				String[] newUsers = (users != null ? users.split(",") : null);
    			List<PrivacyData> toDelete = new LinkedList<PrivacyData>();
    			List<PrivacyData> toAdd = new LinkedList<PrivacyData>();
    			
    	    	String query = "select from " + PrivacyData.class.getName() + " where author == '" + email + "'";
    	    	List<PrivacyData> result = (List<PrivacyData>) pm.newQuery(query).execute();
    	    	for (Iterator<PrivacyData> it=result.iterator(); it.hasNext(); ) {
    	    		PrivacyData data = it.next();
    	    		if (newUsers.length > 0) {
        	    		boolean found = false;
	    	    		for (int i=0; i<newUsers.length; ++i) {
	    	    			if (newUsers[i].equals(data.getAllow())) {
	    	    				found = true;
	    	    				break;
	    	    			}
	    	    		}
	    	    		if (!found)
	    	    			toDelete.add(data);
    	    		}
    	    		else {
    	    			toDelete.add(data);
    	    		}
    	    	}
    	    	
    	    	for (int i=0; i<newUsers.length; ++i) {
    	    		if (newUsers[i] == null || newUsers[i].length() == 0)
    	    			continue;
    	    		boolean found = false;
    	    		for (Iterator<PrivacyData> it=result.iterator(); it.hasNext(); ) {
    	    			PrivacyData data = it.next();
    	    			if (newUsers[i].equals(data.getAllow())) {
    	    				found = true;
    	    				break;
    	    			}
    	    		}
    	    		if (!found) {
    	    			PrivacyData data = new PrivacyData();
    	    			data.setAuthor(email);
    	    			data.setAllow(newUsers[i]);
    	    			toAdd.add(data);
    	    		}
    	    	}
    			
	    		pm.deletePersistentAll(toDelete);
	    		pm.makePersistentAll(toAdd);
    		}
    	} finally {
    		pm.close();
    	}
    }
}
