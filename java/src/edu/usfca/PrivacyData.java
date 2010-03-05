package edu.usfca;

import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * This data object is used to store the privacy information for the 
 * author user. The allow property is the email address of the user whose
 * shared pictured are allowed (viewable) by the author.
 * 
 * @author mamta
 */
@PersistenceCapable
public class PrivacyData {
	@PrimaryKey 
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String author;
	
	@Persistent
	private String allow;

	public Key getKey() {
		return key;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthor() {
		return author;
	}

	public void setAllow(String allow) {
		this.allow = allow;
	}

	public String getAllow() {
		return allow;
	}
}
