package idv.jason.lib.imagemanager;

import java.lang.ref.WeakReference;

import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

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
			if(attr.viewAttr != null) {
				viewAttr.applyWithAnim = attr.viewAttr.applyWithAnim;
				viewAttr.defaultScaleType = attr.viewAttr.defaultScaleType;
				viewAttr.doneScaleType = attr.viewAttr.doneScaleType;
				viewAttr.failScaleType = attr.viewAttr.failScaleType;
				viewAttr.defaultResId = attr.viewAttr.defaultResId;
				viewAttr.failResId = attr.viewAttr.failResId;
				viewAttr.backgroundResId = attr.viewAttr.backgroundResId;
			}
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
	
	public void setFailResId(int resId) {
		if(viewAttr != null && viewAttr.view != null) {
			viewAttr.failResId = resId;
		}
	}
	
	public void setBackground(int resId) {
		if(viewAttr != null && viewAttr.view != null) {
			viewAttr.backgroundResId = resId;
		}
	}
	
	public void setDefaultScaleType(ScaleType type) {
		if(viewAttr != null && viewAttr.defaultResId != -1) {
			viewAttr.defaultScaleType = type;
		}
	}
	
	public void setFailScaleType(ScaleType type) {
		if(viewAttr != null && viewAttr.failResId != -1) {
			viewAttr.failScaleType = type;
		}
	}
	
	public void setDoneScaleType(ScaleType type) {
		if(viewAttr != null ) {
			viewAttr.doneScaleType = type;
		}
	}
	
	public void setLoadFromThread(boolean loadFromThread) {
		if(viewAttr != null ) {
			viewAttr.loadFromThread = loadFromThread;
		}
	}
	
	public ImageView getView() {
		ImageView view = null;
		if(viewAttr != null && viewAttr.view != null) {
			view = viewAttr.view.get();
		}
		return view;
	}
	
	public boolean shouldApplyWithAnimation() {
		if (viewAttr != null) {
			return viewAttr.applyWithAnim;
		}
		return false;
	}

	public void setApplyWithAnimation(boolean value) {
		if (viewAttr != null) {
			viewAttr.applyWithAnim = value;
		}
	}
	
	public boolean shouldLoadFromThread() {
		if(viewAttr != null)
			return viewAttr.loadFromThread;
		return false;
	}
}