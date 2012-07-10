package com.jason.lib.imagemanager.conn;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

public class HttpInvoker {
	public final static String TAG = "HttpInvoker";
	
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		// test for connection
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			return true;
		} else {
			Log.v(TAG, "Internet Connection Not Present");
			return false;
		}
	}
	
	public static String getStringFromUrl(String url) {
		InputStream is = getInputStreamFromUrl(url);
		
		if(is != null) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "Can't decode stream", e);
			}
			if(reader != null) {
				StringBuilder builder = new StringBuilder();
				try {
					for (String line = null; (line = reader.readLine()) != null;) {
						builder.append(line).append("\n");
					}
				} catch (IOException e) {
					Log.e(TAG, "Can't read stream", e);
				}
				return builder.toString();
			}			
		}
		return null;
	}
	
	public static InputStream getInputStreamFromUrl(String url) {
		InputStream content = null;
		HttpGet httpGet = new HttpGet(url);
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(httpGet);
			content = response.getEntity().getContent();
         } catch (IOException e) {
        	 httpGet.abort();
            Log.w(TAG, "I/O error while retrieving bitmap from " + url, e); 
        } catch (IllegalStateException e) {
        	httpGet.abort();
            Log.w(TAG, "Incorrect URL: " + url);
        } catch (Exception e) {
        	httpGet.abort();
            Log.w(TAG, "Error while retrieving bitmap from " + url, e); 
        }
		return content;
	}
	
	/*
     * An InputStream that skips the exact number of bytes provided, unless it reaches EOF.
     */
    public static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }
     
        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break;  // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
}
