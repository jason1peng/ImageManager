package com.jason.demo.imagemanager.image;

import java.lang.ref.WeakReference;

import android.widget.ImageView;

public class ViewAttribute {
	public int defaultResId = -1;
	public int backgroundResId = -1;
	
	WeakReference<ImageView> view;
}
