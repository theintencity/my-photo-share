package edu.usfca;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * The Photo data object that is stored in the datastore persistently.
 * 
 * @author mamta
 */
@PersistenceCapable
public class Photo {
	@PrimaryKey 
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String photo;
	
	@Persistent
	private String author;
	
	@Persistent
	private String caption;
	
	@Persistent
	private Long size;
	
	@Persistent
	private String dimension;
	
	@Persistent
	private Blob thumb;
	
	@Persistent
	private Blob data;

	@Persistent
	private String blobKey;
	
	public Key getKey() {
		return key;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public String getPhoto() {
		return photo;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthor() {
		return author;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public String getCaption() {
		return caption;
	}

	public void setBlobKey(String blobKey) {
		this.blobKey = blobKey;
	}

	public String getBlobKey() {
		return blobKey;
	}

	public void setThumb(Blob thumb) {
		this.thumb = thumb;
	}

	public Blob getThumb() {
		return thumb;
	}

	public void setData(Blob data) {
		this.data = data;
	}

	public Blob getData() {
		return data;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public Long getSize() {
		return size;
	}

	public void setDimension(String dimension) {
		this.dimension = dimension;
	}

	public String getDimension() {
		return dimension;
	}
	
}
