package com.andriipanasiuk.imagedownloader.model;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

public class ImageLoader extends AsyncTask<String, Void, Bitmap> {

	public ImageLoader(ImageView view, LruCache<String, Bitmap> memoryCache) {
		this.view = new WeakReference<ImageView>(view);
		this.memoryCache = memoryCache;
	}

	private WeakReference<ImageView> view;
	private LruCache<String, Bitmap> memoryCache;

	@Override
	protected Bitmap doInBackground(String... params) {
		String path = params[0];
		Bitmap bitmap = BitmapFactory.decodeFile(path);
		if (bitmap != null) {
			memoryCache.put(path, bitmap);
		}
		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (view.get() == null || bitmap == null) {
			return;
		}
		view.get().setImageBitmap(bitmap);
	}

}