/**
 * Copyright 2011 Jason Peng
 * This program is free software under the GNU General Public License.
 * If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package idv.jason.lib.imagemanager;

import android.content.Context;

public class ImageFactory {
	protected ImageFileBasicOperation mOperation;
	public ImageFactory() {
	}
	
	public void setImageBasicOperation(ImageFileBasicOperation operation) {
		mOperation = operation;
	}
	
	public BaseImage getImage(Context context, String url, ImageAttribute attr) {
		BaseImage image = null;
		boolean isLocal = false;
		boolean isDatabase = false;
		boolean isLocalVideo = false;
		if(url.contains(LocalImage.LOCAL_FILE_PREFIX))
			isLocal = true;
		else if(url.contains(MediaStoreImage.PREFIX))
			isDatabase = true;
		else if(url.contains(LocalVideoImage.PREFIX))
			isLocalVideo = true;
		if(isLocal) {
			image = new LocalImage(context, url);
			if(attr != null && attr.containsAttribute()) {
				LocalImage li = (LocalImage) image;
				li.setImaggMaxSize(attr.getMaxHeight(), attr.getMaxWidth());
				li.setHighQuality(attr.highQuality());
			}
		}
		else if(isDatabase) {
			image = new MediaStoreImage(context, url);
		}
		else if(isLocalVideo) {
			image = new LocalVideoImage(url);
		}
		else if(url != null) {
			image = new InternetImage(context, url);
			if(attr != null && attr.containsAttribute()) {
				InternetImage ii = (InternetImage) image;
				ii.setImaggMaxSize(attr.getMaxHeight(), attr.getMaxWidth());
				ii.setHighQuality(true);
			}
		}
		return image;
	}
	
	public BaseImage postProcessImage(Context context, String url,
			ImageAttribute attr, BaseImage image) {
		if (image != null && attr != null && attr.containsAttribute()) {
			if (attr.getResizeWidth() != 0 && attr.getResizeHeight() != 0) {
				image = new ResizeImage(image, attr.getResizeWidth(),
						attr.getResizeHeight());
			}

			if (attr.getBlendRedId() != 0) {
				image = new BlendImage(context, image, attr.getBlendRedId());
			}

			if (attr.getRoundPixels() != 0) {
				image = new RoundCorner(image, attr.getRoundPixels());
			}
			
			if(attr.getBlurRadiout() != 0) {
				image = new Blur(image, attr.getBlurRadiout());
			}
			
			if(attr.getFilter() != 0) {
				image = new FilterImage(image, attr.getFilter());
			}
			
			if(attr.isReflection()) {
				image = new ReflectionImage(image);
			}
		}
		return image;
	}
}
