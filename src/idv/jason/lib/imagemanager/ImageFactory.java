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
		if(url.contains("file://"))
			isLocal = true;
		if(isLocal) {
			if(attr != null && attr.maxHeight != 0 && attr.maxWidth != 0) {
				image = new LocalImage(context, url, attr.maxWidth, attr.maxHeight);
				if(attr.highQuality == true)
					((LocalImage)image).setHighQuality(true);
			} else
				image = new LocalImage(context, url);
		}
		else if(url != null) {
			if(attr != null) {
				image = new InternetImage(context, url, attr.maxWidth, attr.maxHeight);
				if(attr.highQuality == true)
					((InternetImage)image).setHighQuality(true);
			} else
				image = new InternetImage(context, url);
		}
		return image;
	}
	
	public BaseImage postProcessImage(Context context, String url,
			ImageAttribute attr, BaseImage image) {
		if (image != null && attr != null) {
			if (attr.maxHeight != 0 && attr.maxWidth != 0) {
				// we have resized image when decode from local path
				image = new ResizeImage(image, attr.maxWidth,
						attr.maxHeight);
			}

			if (attr.blendResId != 0) {
				image = new BlendImage(context, image, attr.blendResId);
			}

			if (attr.roundPixels != 0) {
				image = new RoundCorner(image, attr.roundPixels);
			}
			
			if(attr.blurRadious != 0) {
				image = new Blur(image, attr.blurRadious);
			}
		}
		return image;
	}
}
