package com.andriipanasiuk.imagedownloader;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.andriipanasiuk.imagedownloader.model.PreviewAdapter;
import com.andriipanasiuk.imagedownloader.service.DownloadService;
import com.andriipanasiuk.imagedownloader.service.DownloadService.DownloadBinder;

public class MainActivity extends ActionBarActivity implements OnClickListener {

	public static final String LOG_TAG = "ImageDownloader";
	private boolean bound = false;
	private boolean serviceStopped = false;
	private DownloadService downloadService;
	private ProgressReceiver receiver;
	private ListView imageListView;
	private Button downloadButton;
	private PreviewAdapter adapter;

	private LruCache<String, Bitmap> memoryCache;

	private final ServiceConnection downloadServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			bound = false;
			disableUI();
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(LOG_TAG, "onServiceConnected");
			downloadService = ((DownloadBinder) service).getService();
			bound = true;
			enableUI();
			adapter.updateData(downloadService.getDownloads());
			imageListView.setAdapter(adapter);
		}
	};

	private class ProgressReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(LOG_TAG, "onReceive");
			int position = intent.getIntExtra(DownloadService.DOWNLOAD_ID_KEY, -1);
			if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_PROGRESS)) {
				adapter.updateItem(imageListView, position);
			} else if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_COMPLETE)) {
				adapter.updateItem(imageListView, position);
				Toast.makeText(MainActivity.this, R.string.download_complete, Toast.LENGTH_SHORT).show();
			} else if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_ERROR)) {
				adapter.updateItem(imageListView, position);
				Toast.makeText(MainActivity.this, R.string.error_while_downloading, Toast.LENGTH_SHORT).show();
			} else if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_CANCELLED)) {
				adapter.notifyDataSetChanged();
			}
		}
	}

	private void stopService() {
		downloadService.stopNow();
		unbindService();
		serviceStopped = true;
	}

	private void startService() {
		Intent intent = new Intent(this, DownloadService.class);
		startService(intent);
		serviceStopped = false;
	}

	private void bindService() {
		Intent intent = new Intent(this, DownloadService.class);
		bindService(intent, downloadServiceConnection, Context.BIND_AUTO_CREATE);
		receiver = new ProgressReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_DOWNLOAD_PROGRESS);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_COMPLETE);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_ERROR);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_CANCELLED);
		registerReceiver(receiver, filter);
	}

	private void unbindService() {
		if (bound) {
			unbindService(downloadServiceConnection);
			bound = false;
			unregisterReceiver(receiver);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			serviceStopped = savedInstanceState.getBoolean("serviceStoppedKey");
			Log.d(LOG_TAG, "onRestoreInstanceState " + serviceStopped);
		}
		if (!serviceStopped) {
			startService();
		}

		RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(getFragmentManager());
		memoryCache = retainFragment.mRetainedCache;
		if (memoryCache == null) {
			memoryCache = new LruCache<String, Bitmap>(20);
			retainFragment.mRetainedCache = memoryCache;
		}

		setContentView(R.layout.activity_main);
		downloadButton = (Button) findViewById(R.id.download_button);
		downloadButton.setOnClickListener(this);
		imageListView = (ListView) findViewById(R.id.image_list);
		adapter = new PreviewAdapter(this, memoryCache);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		Log.d(LOG_TAG, "onSaveInstanceState " + serviceStopped);
		state.putBoolean("serviceStoppedKey", serviceStopped);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!serviceStopped) {
			bindService();
		}
	}

	@Override
	public void onStop() {
		if (!serviceStopped) {
			unbindService();
		}
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (serviceStopped) {
			menu.add(R.string.start_service);
			menu.getItem(0).setChecked(false);
		} else {
			menu.add(R.string.stop_service);
			menu.getItem(0).setChecked(true);
		}
		return true;
	}

	private void disableUI() {
		downloadButton.setEnabled(false);
	}

	private void enableUI() {
		downloadButton.setEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		int order = item.getOrder();
		if (order == 0) {
			if (item.isChecked()) {
				stopService();
				adapter.notifyDataSetChanged();
				Toast.makeText(this, R.string.service_was_stopped, Toast.LENGTH_SHORT).show();
				item.setChecked(false);
				item.setTitle(R.string.start_service);
			} else {
				startService();
				bindService();
				item.setChecked(true);
				item.setTitle(R.string.stop_service);
			}
			Log.d(LOG_TAG, "Click on start/stop service");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.download_button) {
			if (bound) {
				String url = ((EditText) findViewById(R.id.download_url)).getText().toString();
				boolean result = downloadService.downloadImage(url);
				if (result) {
					adapter.updateData(downloadService.getDownloads());
				} else {
					Toast.makeText(this, R.string.service_is_stopped, Toast.LENGTH_SHORT).show();
				}
			} else if (serviceStopped) {
				Toast.makeText(this, R.string.service_is_stopped, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, R.string.service_is_starting, Toast.LENGTH_SHORT).show();
			}
		}
	}
}
