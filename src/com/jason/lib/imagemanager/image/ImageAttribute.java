package com.jason.lib.imagemanager.image;

import java.lang.ref.WeakReference;

import android.widget.ImageView;

public class ImageAttribute {
	public int thumbWidth = 0;
	public int thumbHeight = 0;	
	
	public int roundPixels = 0;
	
	public int blendResId = 0;
	
	public ImageAttribute() {
		
	}
	
	public ImageAttribute(ImageView view) {
		if(view != null) {
			viewAttr = new ViewAttribute();
			viewAttr.view = new WeakReference<ImageView>(view);
		}
	}
	
	public ImageAttribute(ImageAttribute attr, ImageView view) {
		this.thumbHeight = attr.thumbHeight;
		this.thumbWidth = attr.thumbWidth;
		this.roundPixels = attr.roundPixels;
		this.blendResId = attr.blendResId;
		if(view != null) {
			viewAttr = new ViewAttribute();
			viewAttr.view = new WeakReference<ImageView>(view);
		}
	}
	
	public String getStringAttr() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(thumbWidth);
		builder.append(thumbHeight);
		builder.append(roundPixels);
		builder.append(blendResId);
		
		return builder.toString();
	}
	
	public ViewAttribute viewAttr;
	
	public void setDefaultResId(int resId) {
		if(viewAttr != null && viewAttr.view != null) {
			viewAttr.defaultResId = resId;
		}
	}
	
	public void setBackground(int resId) {
		if(viewAttr != null && viewAttr.view != null) {
			viewAttr.backgroundResId = resId;
		}
	}
	
	public ImageView getView() {
		ImageView view = null;
		if(viewAttr != null && viewAttr.view != null) {
			view = viewAttr.view.get();
		}
		return view;
	}
}