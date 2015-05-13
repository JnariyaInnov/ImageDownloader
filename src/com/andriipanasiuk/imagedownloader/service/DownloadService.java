package com.andriipanasiuk.imagedownloader.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore.Images;
import android.util.Log;

import com.andriipanasiuk.imagedownloader.MainActivity;
import com.andriipanasiuk.imagedownloader.model.DB;
import com.andriipanasiuk.imagedownloader.model.DownloadInfo;

public class DownloadService extends Service {

	private static final int HEIGHT = 480;
	private static final int WIDTH = 320;

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

	private State state = State.STOPPED;

	public static enum State {
		RUNNING, STOPPING, STOPPED
	}

	@Override
	public void onCreate() {
		super.onCreate();
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
		state = State.RUNNING;
		return START_NOT_STICKY;
	}

	/**
	 * Stop service immediately. All unfinished downloads will be interrupted.
	 */
	public void stopNow() {
		executor.shutdownNow();
		synchronized (stoppingLock) {
			state = State.STOPPED;
		}
		stopSelf();
	}

	/**
	 * Stop service after unfinished downloads will be done.
	 */
	public void stop() {
		state = State.STOPPING;
		executor.shutdown();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					// do nothing
				}
				stopSelf();
				state = State.STOPPED;
			}
		}).start();
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

	private File downloadImageInternal(String urlString, DownloadListener listener) throws IOException {
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
		while ((count = input.read(data)) != -1 && state != State.STOPPED) {
			outputStream.write(data, 0, count);
			total += count;
			synchronized (stoppingLock) {
				listener.onProgress(total, fileSize);
			}
		}
		outputStream.flush();
		outputStream.close();
		input.close();
		if (state != State.STOPPED) {
			Log.d(MainActivity.LOG_TAG, cacheDownloadFile.getAbsolutePath());
			return cacheDownloadFile;
		} else {
			listener.onCancelled();
			cacheDownloadFile.delete();
			return null;
		}
	}

	private String saveToSD(Bitmap bitmap, String title) throws IOException {
		File albumDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"ImageDownloader");
		if (!albumDirectory.exists()) {
			albumDirectory.mkdirs();
		}
		File savedImage = new File(albumDirectory, createNameForImage(title));
		Log.d(MainActivity.LOG_TAG, "Saved path: " + savedImage.getAbsolutePath());
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
				info.state = DownloadInfo.State.PROCESS;

				File originalImageFile = downloadImageInternal(info.url, listener);
				if (state == State.STOPPED || originalImageFile == null) {
					return;
				}
				Bitmap scaledBitmap = ImageUtil.createScaled(originalImageFile, WIDTH, HEIGHT);
				originalImageFile.delete();
				String path = saveToSD(scaledBitmap, info.url);
				publishOnGallery(path, info.url);

				info.state = DownloadInfo.State.COMPLETE;
				info.path = path;
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

	@SuppressWarnings("unused")
	private class StubDownloadRunnable implements Runnable {
		private final DownloadInfo info;
		private final DownloadListener listener;

		@Override
		public void run() {
			try {
				info.state = DownloadInfo.State.PROCESS;
				for (int i = 0; i < 5; i++) {
					Thread.sleep(1000);
					listener.onProgress(1000 * (i + 1), 1000 * 5);
				}
				if (state == State.STOPPED) {
					return;
				}
				info.state = DownloadInfo.State.COMPLETE;
				synchronized (stoppingLock) {
					listener.onComplete("");
				}
			} catch (InterruptedException e) {
				// do nothing
			}
		}

		public StubDownloadRunnable(DownloadInfo info, DownloadListener listener) {
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
		if (state != State.RUNNING) {
			return false;
		}
		DownloadInfo info = new DownloadInfo();
		info.url = url;
		info.state = DownloadInfo.State.WAITING;
		int id = DB.getInstance().addDownload(info);
		executor.execute(new DownloadRunnable(info, new DownloadInfoSender(info, id)));
		return true;
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
			info.state = DownloadInfo.State.ERROR;
			Intent errorIntent = new Intent(ACTION_DOWNLOAD_ERROR);
			errorIntent.putExtra(DOWNLOAD_ID_KEY, id);
			sendBroadcast(errorIntent);
		}

		@Override
		public void onComplete(String path) {
			Intent completeIntent = new Intent(ACTION_DOWNLOAD_COMPLETE);
			completeIntent.putExtra(DOWNLOAD_ID_KEY, id);
			sendBroadcast(completeIntent);
		}

		@Override
		public void onCancelled() {
			Log.d(MainActivity.LOG_TAG, "onCancelled");
			info.state = DownloadInfo.State.CANCELLED;
			Intent cancelledIntent = new Intent(ACTION_DOWNLOAD_CANCELLED);
			cancelledIntent.putExtra(DOWNLOAD_ID_KEY, id);
			sendBroadcast(cancelledIntent);
		}
	}

}
