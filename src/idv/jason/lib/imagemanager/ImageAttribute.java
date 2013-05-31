package idv.jason.lib.imagemanager;

import idv.jason.lib.imagemanager.ImageManager.ImageDoneCallback;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageAttribute {
	public int maxWidth = 0;
	public int maxHeight = 0;
	
	private int resizeWidth = 0;
	private int resizeHeight = 0;	
	
	private int roundPixels = 0;
	private int blendResId = 0;
	
	private int blurRadious = 0;
	
	private boolean highQuality = false;
	
	private boolean defaultAttribute = true;
	
	public int filterPhoto = 0;
	
	public boolean reflection = false;
	
	public ImageDoneCallback mCallback;
	public Object mParam;
	
	public ImageAttribute() {
		
	}
	
	public ImageAttribute(ImageDoneCallback callback) {
		mCallback = callback;
	}
	
	public void setCallbackParam(Object param) {
		mParam = param;
	}
	
	public ImageAttribute(ImageView view) {
		if(view != null) {
			viewAttr = new ViewAttribute();
			viewAttr.view = new WeakReference<ImageView>(view);
		}
	}
	
	public ImageAttribute(ImageView view, ImageDoneCallback callback) {
		if(view != null) {
			viewAttr = new ViewAttribute();
			viewAttr.view = new WeakReference<ImageView>(view);
		}
		mCallback = callback;
	}
	
	public int getMaxWidth() {
		return maxWidth;
	}
	
	public int getMaxHeight() {
		return maxHeight;
	}
	
	public int getResizeWidth() {
		return resizeWidth;
	}
	
	public int getResizeHeight() {
		return resizeHeight;
	}
	
	public int getRoundPixels() {
		return roundPixels;
	}
	
	public int getBlendRedId() {
		return blendResId;
	}
	
	public int getBlurRadiout() {
		return blurRadious;
	}
	
	public boolean highQuality() {
		return highQuality;
	}
	
	public boolean reflection() {
		return reflection;
	}
	
	public void setMaxSize(int width, int height) {
		maxWidth = width;
		maxHeight = height;
		defaultAttribute = false;
	}
	
	public void setResizeSize(int width, int height) {
		resizeWidth = width;
		resizeHeight = height;
		defaultAttribute = false;
	}
	
	public void setRoundPixels(int round) {
		roundPixels = round;
		defaultAttribute = false;
	}
	
	public void setBlendResId(int blend) {
		blendResId = blend;
		defaultAttribute = false;
	}
	
	public void setBlurRadious(int blur) {
		blurRadious = blur;
		defaultAttribute = false;
	}
	

	public void setHighQuality(boolean quality) {
		highQuality = quality;
		defaultAttribute = false;
	}
	
	public void setReflection(boolean reflect) {
		reflection = reflect;
		defaultAttribute = false;
	}
	
	public void setFilter(int filter) {
		filterPhoto = filter;
		defaultAttribute = false;
	}
	
	public ImageAttribute(ImageAttribute attr, ImageView view) {
		this.maxHeight = attr.maxHeight;
		this.maxWidth = attr.maxWidth;
		this.resizeHeight = attr.resizeHeight;
		this.resizeWidth = attr.resizeWidth;
		this.roundPixels = attr.roundPixels;
		this.blendResId = attr.blendResId;
		this.highQuality = attr.highQuality;
		this.blurRadious = attr.blurRadious;
		
		this.filterPhoto = attr.filterPhoto;
		if(view != null) {
			viewAttr = new ViewAttribute();
			viewAttr.view = new WeakReference<ImageView>(view);
		}
	}
	
	public String getStringAttr() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(maxWidth);
		builder.append(maxHeight);
		builder.append(resizeWidth);
		builder.append(resizeHeight);
		builder.append(roundPixels);
		builder.append(blendResId);
		builder.append(highQuality);
		builder.append(blurRadious);

		builder.append(filterPhoto);
		builder.append(reflection);
		
		return builder.toString();
	}
	
	public boolean containsAttribute() {
		return !defaultAttribute;
	}
	
	public ViewAttribute viewAttr;
	
	public void setDefaultColor(int defaultColor) {
		if(viewAttr != null && viewAttr.view != null) {
			viewAttr.defaultColor = defaultColor;
		}
	}
	
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
	
	public void setMaxSizeEqualsScreenSize(Context context) {
		DisplayMetrics display = context.getResources().getDisplayMetrics();
		setMaxSize(display.widthPixels, display.heightPixels);
	}
}
