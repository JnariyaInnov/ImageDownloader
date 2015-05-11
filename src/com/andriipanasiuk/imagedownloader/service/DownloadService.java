package com.andriipanasiuk.imagedownloader.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore.Images;
import android.util.Log;

import com.andriipanasiuk.imagedownloader.MainActivity;
import com.andriipanasiuk.imagedownloader.model.DownloadInfo;
import com.andriipanasiuk.imagedownloader.model.DownloadInfo.State;

public class DownloadService extends Service {

	private static interface DownloadListener {
		void onProgress(int downloaded, int size);

		void onComplete(String path);

		void onError();

		void onCancelled();
	}

	private final IBinder binder = new DownloadBinder();
	private ExecutorService executor;
	private Object stoppingLock = new Object();

	public static final String ACTION_DOWNLOAD_PROGRESS = "download_progress";
	public static final String ACTION_DOWNLOAD_COMPLETE = "download_complete";
	public static final String ACTION_DOWNLOAD_CANCELLED = "download_cancelled";
	public static final String ACTION_DOWNLOAD_ERROR = "download_error";
	public static final String IMAGE_PATH_KEY = "image_path_key";
	public static final String PROGRESS_KEY = "progress_key";
	public static final String DOWNLOADED_BYTES_KEY = "downloaded_bytes_key";
	public static final String DOWNLOAD_ID_KEY = "downloaded_id_key";
	public static final String ALL_BYTES_KEY = "all_bytes_key";

	private List<DownloadInfo> downloads;
	private boolean stopped = true;

	@Override
	public void onCreate() {
		super.onCreate();
		downloads = new ArrayList<DownloadInfo>();
		executor = Executors.newFixedThreadPool(5);
		Log.d(MainActivity.LOG_TAG, "onCreate " + this);
	}

	@Override
	public void onDestroy() {
		Log.d(MainActivity.LOG_TAG, "onDestroy " + this);
		executor.shutdownNow();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainActivity.LOG_TAG, "onStartCommand " + this);
		if (executor.isShutdown()) {
			executor = Executors.newFixedThreadPool(5);
		}
		stopped = false;
		return START_NOT_STICKY;
	}

	public void stopNow() {
		executor.shutdownNow();
		synchronized (stoppingLock) {
			downloads.clear();
			stopped = true;
		}
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(MainActivity.LOG_TAG, "onBind");
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(MainActivity.LOG_TAG, "onUnbind");
		return true;
	}

	private String createNameForImage(String url) {
		return System.currentTimeMillis() + "_" + url.replaceAll("/", "_");
	}

	private Bitmap downloadImageInternal(String urlString, DownloadListener listener) throws IOException {
		URL url = new URL(urlString);
		URLConnection connection = url.openConnection();
		connection.connect();
		int fileSize = connection.getContentLength();
		Log.d(MainActivity.LOG_TAG, "Image size in bytes: " + fileSize);
		InputStream input = new BufferedInputStream(url.openStream());
		File cacheDownloadFile = new File(getCacheDir(), createNameForImage(urlString));
		FileOutputStream outputStream = new FileOutputStream(cacheDownloadFile);
		byte data[] = new byte[1024];
		int count = 0;
		int total = 0;
		while ((count = input.read(data)) != -1 && !stopped) {
			outputStream.write(data, 0, count);
			total += count;
			synchronized (stoppingLock) {
				listener.onProgress(total, fileSize);
			}
		}
		Bitmap bitmap = null;
		if (!stopped) {
			bitmap = BitmapFactory.decodeFile(cacheDownloadFile.getAbsolutePath());
		}
		outputStream.flush();
		outputStream.close();
		cacheDownloadFile.delete();
		input.close();
		return bitmap;
	}

	private Bitmap resize(Bitmap original) {
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, 320, 480, false);
		return scaledBitmap;
	}

	private String saveToSD(Bitmap bitmap, String title) throws IOException {
		File albumDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"ImageDownloader");
		if (!albumDirectory.exists()) {
			albumDirectory.mkdirs();
		}
		File savedImage = new File(albumDirectory, createNameForImage(title));
		Log.d(MainActivity.LOG_TAG, "Path: " + savedImage.getAbsolutePath());
		FileOutputStream stream = new FileOutputStream(savedImage);
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		return savedImage.getAbsolutePath();
	}

	private void publishOnGallery(String path, String title) throws IOException {
		ContentValues values = new ContentValues(7);

		values.put(Images.Media.TITLE, title);
		values.put(Images.Media.DISPLAY_NAME, title);
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.ORIENTATION, 0);
		values.put(Images.Media.DATA, path);
		getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	private class DownloadRunnable implements Runnable {
		private final DownloadInfo info;
		private final DownloadListener listener;

		@Override
		public void run() {
			try {
				info.state = State.PROCESS;
				Bitmap bitmap = downloadImageInternal(info.url, listener);
				if (stopped) {
					return;
				}
				Bitmap scaledBitmap = resize(bitmap);
				bitmap.recycle();
				String path = saveToSD(scaledBitmap, info.url);
				publishOnGallery(path, info.url);
				synchronized (stoppingLock) {
					listener.onComplete(path);
				}
			} catch (IOException e) {
				synchronized (stoppingLock) {
					listener.onError();
				}
				Log.e(MainActivity.LOG_TAG, "Exception while image downloading", e);
			}

		}

		public DownloadRunnable(DownloadInfo info, DownloadListener listener) {
			super();
			this.info = info;
			this.listener = listener;
		}
	}

	public class DownloadBinder extends Binder {
		public DownloadService getService() {
			return DownloadService.this;
		}
	}

	public synchronized boolean downloadImage(String url) {
		if (stopped) {
			return false;
		}
		DownloadInfo info = new DownloadInfo();
		info.url = url;
		info.state = State.WAITING;
		downloads.add(info);
		executor.execute(new DownloadRunnable(info, new DownloadInfoSender(info, downloads.size() - 1)));
		return true;
	}

	public List<DownloadInfo> getDownloads() {
		if (stopped) {
			return Collections.emptyList();
		}
		return downloads;
	}

	private class DownloadInfoSender implements DownloadListener {
		private final Intent progressIntent;
		private long lastUpdate;
		private DownloadInfo info;
		private int id;
		private static final long UPDATE_INTERVAL = 400;

		public DownloadInfoSender(DownloadInfo info, int id) {
			progressIntent = new Intent(ACTION_DOWNLOAD_PROGRESS);
			this.info = info;
			this.id = id;
		}

		@Override
		public void onProgress(int downloaded, int size) {
			if (System.currentTimeMillis() - lastUpdate > UPDATE_INTERVAL) {
				int progress = downloaded * 100 / size;
				info.progress = progress;
				info.downloadedBytes = downloaded;
				info.allBytes = size;
				progressIntent.putExtra(DOWNLOAD_ID_KEY, id);
				sendBroadcast(progressIntent);
				lastUpdate = System.currentTimeMillis();
			}
		}

		@Override
		public void onError() {
			info.state = State.ERROR;
			Intent errorIntent = new Intent(ACTION_DOWNLOAD_ERROR);
			errorIntent.putExtra(DOWNLOAD_ID_KEY, id);
			sendBroadcast(errorIntent);
		}

		@Override
		public void onComplete(String path) {
			info.state = State.COMPLETE;
			info.url = path;
			Intent completeIntent = new Intent(ACTION_DOWNLOAD_COMPLETE);
			completeIntent.putExtra(DOWNLOAD_ID_KEY, id);
			sendBroadcast(completeIntent);
		}

		@Override
		public void onCancelled() {
			info.state = State.CANCELLED;
			Intent cancelledIntent = new Intent(ACTION_DOWNLOAD_CANCELLED);
			cancelledIntent.putExtra(DOWNLOAD_ID_KEY, id);
			sendBroadcast(cancelledIntent);
		}
	}

}
