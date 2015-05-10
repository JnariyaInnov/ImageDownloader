package com.andriipanasiuk.imagedownloader.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.andriipanasiuk.imagedownloader.MainActivity;

public class DownloadService extends Service {

	private final IBinder binder = new DownloadInteractor();
	private ExecutorService executor;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		executor = Executors.newFixedThreadPool(5);
		Log.d(MainActivity.LOG_TAG, "onStartCommand");
		return START_NOT_STICKY;
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

	public class DownloadInteractor extends Binder {
		public void downloadImage(String url) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// do nothing
					}
				}
			});
		}
	}

}
