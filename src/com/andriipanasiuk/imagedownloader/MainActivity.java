package com.andriipanasiuk.imagedownloader;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.andriipanasiuk.imagedownloader.service.DownloadService;
import com.andriipanasiuk.imagedownloader.service.DownloadService.DownloadInteractor;

public class MainActivity extends ActionBarActivity implements OnClickListener {

	public static final String LOG_TAG = "ImageDownloader";
	private boolean bound = false;
	private DownloadInteractor downloadInteractor;
	private ProgressReceiver receiver;
	private ProgressBar progressBar;
	private TextView progressText;
	private ImageView previewIV;

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

	private class ProgressReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int progress = intent.getIntExtra(DownloadService.PROGRESS_KEY, -1);
			int downloadedBytes = intent.getIntExtra(DownloadService.DOWNLOADED_BYTES_KEY, -1);
			int allBytes = intent.getIntExtra(DownloadService.ALL_BYTES_KEY, -1);
//			Log.d(LOG_TAG, "Action: " + intent.getAction() + " progress: " + progress);
			if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_PROGRESS)) {
				progressBar.setProgress(progress);
				progressText.setText(bytesToUIString(downloadedBytes) + "/" + bytesToUIString(allBytes));
			} else if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_COMPLETE)) {
				String path = intent.getStringExtra(DownloadService.IMAGE_PATH_KEY);
				Bitmap bitmap = BitmapFactory.decodeFile(path);
				previewIV.setImageBitmap(bitmap);
				progressBar.setProgress(100);
				Toast.makeText(MainActivity.this, "Download complete", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private String bytesToUIString(int bytes) {
		int kbytes = bytes / 1024;
		if (kbytes < 1024) {
			return kbytes + "KB";
		}
		int mbytes = kbytes / 1024;
		return mbytes + "," + kbytes % 1024 + "MB";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, DownloadService.class);
		startService(intent);

		setContentView(R.layout.activity_main);
		findViewById(R.id.download_button).setOnClickListener(this);
		previewIV = (ImageView) findViewById(R.id.preview_image);
		progressBar = (ProgressBar) findViewById(R.id.download_progress);
		progressText = (TextView) findViewById(R.id.download_progress_text);
	}

	@Override
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(this, DownloadService.class);
		bindService(intent, downloadServiceConnection, Context.BIND_AUTO_CREATE);

		receiver = new ProgressReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_DOWNLOAD_PROGRESS);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_COMPLETE);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_ERROR);
		registerReceiver(receiver, filter);
	}

	@Override
	public void onStop() {
		if (bound) {
			unbindService(downloadServiceConnection);
			bound = false;
		}
		unregisterReceiver(receiver);
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

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.download_button) {
			downloadInteractor.downloadImage(((EditText) findViewById(R.id.download_url)).getText().toString(), 0);
		}
	}
}
