/**
 * Copyright 2011 Jason Peng
 * This program is free software under the GNU General Public License.
 * If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package com.jason.lib.imagemanager.image;

import android.content.Context;

public class ImageFactory {
	
	public static BaseImage getImage(Context context, String url, ImageAttribute attr) {
		BaseImage image = null;
		boolean isLocal = false;
		if(url.contains("file://"))
			isLocal = true;
		if(isLocal) {
			if(attr != null && attr.thumbHeight != 0 && attr.thumbWidth != 0)
				image = new LocalImage(context, url, attr.thumbWidth, attr.thumbHeight);
			else
				image = new LocalImage(context, url);
		}
		else if(url != null) {
			image = new InternetImage(context, url);
		}
		
		if(image != null && attr != null) {
			if(attr.thumbHeight != 0 && attr.thumbWidth != 0 && isLocal==false) {
				// we have resized image when decode from local path
				image = new ResizeImage(image, attr.thumbWidth, attr.thumbHeight);
			}
			
			if(attr.blendResId != 0) {
				image = new BlendImage(context, image, attr.blendResId);
			}			

			if(attr.roundPixels != 0) {
				  image = new RoundCorner(image, attr.roundPixels);
			}
		}		
		
		return image;
	}
}
