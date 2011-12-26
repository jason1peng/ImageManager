package com.jason.demo.imagemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.jason.demo.imagemanager.image.ImageAttribute;
import com.jason.demo.imagemanager.image.ImageManager;
import com.jason.demo.imagemanager.image.ImageManager.ImageManagerCallback;
import com.jason.demo.imagemanager.imagesource.YoutubeImageSourceGetter;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageManagerActivity extends ListActivity implements ImageManagerCallback {
	public static final String TAG = ImageManagerActivity.class.getSimpleName();
	private ImageManager mImageManager;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mImageManager = ImageManager.getInstance(this);
        mImageManager.registerCallback(this);
        
        IntentFilter filter = new IntentFilter(YoutubeImageSourceGetter.ACTION_IMAGES_DONE);
        this.registerReceiver(mReceiver, filter);
        new YoutubeImageSourceGetter(this).execute();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.clear_cache:
        	mImageManager.clearCache();
        	File dir = this.getCacheDir();
        	if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public static boolean deleteDir(File dir) {
        if (dir!=null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }        return dir.delete();
    }
    
    class ImageAdapter extends BaseAdapter {
    	private LayoutInflater mInflater;
    	ArrayList<HashMap<String, String>> mList;
    	
    	public ImageAdapter(Context context, ArrayList<HashMap<String, String>> list) {
    		this.mList = list;
    		this.mInflater = LayoutInflater.from(context);
    	}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			String title = mList.get(position).get("title");
			String thumbnail = mList.get(position).get("thumbnail");
			
			if(convertView == null) {
				convertView = mInflater.inflate(R.layout.listview_item, null);
			}

			TextView tv = (TextView) convertView.findViewById(R.id.title);
			tv.setText(title);
			
			
			ImageView iv = (ImageView) convertView.findViewById(R.id.thumbnail);
			ImageAttribute attr = new ImageAttribute(iv);
			attr.thumbHeight = 100;
			attr.thumbWidth = 100;
			mImageManager.getImage(thumbnail, attr);
			
			return convertView;
		}
    	
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			ArrayList<String> titleList = intent.getStringArrayListExtra(YoutubeImageSourceGetter.DATA_TITLES);
			ArrayList<String> imageList = intent.getStringArrayListExtra(YoutubeImageSourceGetter.DATA_IMAGES);
			ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
			for(int i=0; i<titleList.size(); i++) {
				HashMap<String, String> item = new HashMap<String, String>();
				item.put("title", titleList.get(i));
				item.put("thumbnail", imageList.get(i));
				list.add(item);
			}
			unregisterReceiver(this);
			setListAdapter(new ImageAdapter(ImageManagerActivity.this, list));
		}
	};

	@Override
	public void imageDone(String id, Bitmap bitmap) {
		Log.d(TAG, id + " downloaded");
	}
}