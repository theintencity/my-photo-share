package edu.usfca;

import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * This data object is stored persistently in data store to represent a sharing
 * information of a photo with another user by the owner of the share.
 * The owner may be different than the author of the photo if the photo is
 * shared consecutively from one person to another to third. The owner field
 * is the sharer email address and user field is the share with email.
 * 
 * @author mamta
 *
 */
@PersistenceCapable
public class ShareData {
	@PrimaryKey 
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String photo;
	
	@Persistent
	private String owner;
	
	@Persistent
	private String user;

	public Key getKey() {
		return key;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public String getPhoto() {
		return photo;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getOwner() {
		return owner;
	}
}
