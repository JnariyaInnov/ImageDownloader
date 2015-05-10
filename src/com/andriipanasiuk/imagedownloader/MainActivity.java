package com.andriipanasiuk.imagedownloader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.andriipanasiuk.imagedownloader.service.DownloadService;
import com.andriipanasiuk.imagedownloader.service.DownloadService.DownloadInteractor;

public class MainActivity extends ActionBarActivity {

	public static final String LOG_TAG = "ImageDownloader";
	private boolean bound = false;
	private DownloadInteractor downloadInteractor;
	private final ServiceConnection downloadServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			bound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(LOG_TAG, "onServiceConnected");
			downloadInteractor = (DownloadInteractor) service;
			bound = true;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, DownloadService.class);
		startService(intent);
		setContentView(R.layout.activity_main);
	}

	@Override
	public void onStart(){
		super.onStart();
		Intent intent = new Intent(this, DownloadService.class);
		bindService(intent, downloadServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		if (bound) {
			unbindService(downloadServiceConnection);
			bound = false;
		}
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Stop service");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int order = item.getOrder();
		if (order == 0) {
			Log.d(LOG_TAG, "Click on start/stop service");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
