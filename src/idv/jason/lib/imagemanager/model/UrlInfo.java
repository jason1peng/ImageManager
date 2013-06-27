package idv.jason.lib.imagemanager.model;

import idv.jason.lib.imagemanager.LocalImage;
import idv.jason.lib.imagemanager.MediaStoreImage;

public class UrlInfo {
	String path;
	String url;
	String imageId;
	
	public UrlInfo(String url) {
		this.path = null;
		this.url = url;
	}
	
	public UrlInfo(String path, String url) {
		this.path = path;
		this.url = url;
	}
	
	public void setImageId(String id) {
		imageId = id;
	}
	
	public String getDownloadUrl() {
		return url;
	}
	
	public String getUniquePath() {
		if(path == null)
			return url;
		else
			return path;
	}
	
	public String getImageId() {
		return imageId;
	}
	
	public boolean isLocalFile() {
		return url.contains(LocalImage.LOCAL_FILE_PREFIX);
	}
	
	public boolean isMediaStoreFile() {
		return url.contains(MediaStoreImage.PREFIX);
	}
	
	public boolean isInternetFile() {
		return url.contains("http") || url.contains("https"); 
	}
}