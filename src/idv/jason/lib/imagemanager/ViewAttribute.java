package idv.jason.lib.imagemanager;

import java.lang.ref.WeakReference;

import android.widget.ImageView;

public class ViewAttribute {
	public int defaultResId = -1;
	public int backgroundResId = -1;
	
	public boolean applyWithAnim = false;
	
	WeakReference<ImageView> view;
}
