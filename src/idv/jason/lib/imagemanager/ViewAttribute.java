package idv.jason.lib.imagemanager;

import java.lang.ref.WeakReference;

import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ViewAttribute {
	public int defaultResId = -1;
	public int backgroundResId = -1;
	public int failResId = -1;
	
	public ScaleType defaultScaleType = ScaleType.FIT_XY;
	public ScaleType failScaleType = ScaleType.CENTER;
	public ScaleType doneScaleType = ScaleType.CENTER;
	
	public boolean applyWithAnim = false;
	public boolean loadFromThread = false;
	
	WeakReference<ImageView> view;
}
