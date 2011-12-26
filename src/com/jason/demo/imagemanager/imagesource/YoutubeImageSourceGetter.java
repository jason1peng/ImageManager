package com.jason.demo.imagemanager.imagesource;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

public class YoutubeImageSourceGetter extends AsyncTask<Void, Void, Void>{
	public static final String TAG = "YoutubeImageSourceGetter";
	public static final String ACTION_IMAGES_DONE = "com.jason.demo.imagemanager.action.GET_IMAGES_DONE";
	public static final String DATA_TITLES = "com.jason.demo.imagemanager.data.TITLES";
	public static final String DATA_IMAGES = "com.jason.demo.imagemanager.data.IMAGES";

	public static final String URL_YOUTUBE_CHANNEL = "https://gdata.youtube.com/feeds/api";
	public static final String URL_YOUTUBE_CHANNEL_MOST_VIEWED = URL_YOUTUBE_CHANNEL + "/standardfeeds/most_viewed?v=2&alt=jsonc&strict=true&time=today&max-results=50";
	
	private Context mContext;
	private ArrayList<String> mTitles = null;
	private ArrayList<String> mThumbnails = null;
	
	public YoutubeImageSourceGetter(Context context) {
		mContext = context;
	}
	
	@Override
	protected Void doInBackground(Void... arg0) {
		String url= URL_YOUTUBE_CHANNEL_MOST_VIEWED;
		String jsonOutput = HttpInvoker.getStringFromUrl(url);
		if(jsonOutput!=null && !jsonOutput.equals("ConnectTimeoutException")) {
			YoutubeDataEntity feeds = null;
			feeds = new Gson().fromJson(jsonOutput, YoutubeDataEntity.class);
			if(feeds != null) {
				mThumbnails = new ArrayList<String>();
				mTitles = new ArrayList<String>();
				for(YoutubeDataEntity.ItemEntity entity : feeds.info.items) {
					mTitles.add(entity.title);
					mThumbnails.add(entity.thumbnail.hqDefault);
				}
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void param) {
		Intent intent = new Intent(ACTION_IMAGES_DONE);
		intent.putStringArrayListExtra(DATA_TITLES, mTitles);
		intent.putStringArrayListExtra(DATA_IMAGES, mThumbnails);
		mContext.sendBroadcast(intent);
    }
	
	public class YoutubeDataEntity {
		public class ThumbnailEntity {
			@SerializedName("sqDefault")
			public String sqDefault;
			
			@SerializedName("hqDefault")
			public String hqDefault;
		}
		
		public class ItemEntity {			
			@SerializedName("title")
			public String title; 
			
			@SerializedName("thumbnail")
			public ThumbnailEntity thumbnail;
		}
		
		public class InformationEntity {
			@SerializedName("items")
			public List<ItemEntity> items;	
		};

		@SerializedName("data")
		public InformationEntity info;	
	}
}
