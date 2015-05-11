package com.andriipanasiuk.imagedownloader;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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
	private DownloadService downloadService;
	private ProgressReceiver receiver;
	private ListView imageListView;
	private Button downloadButton;
	private PreviewAdapter adapter;

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
			int position = intent.getIntExtra(DownloadService.DOWNLOAD_ID_KEY, -1);
			if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_PROGRESS)) {
				adapter.updateItem(imageListView, position);
			} else if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_COMPLETE)) {
				adapter.updateItem(imageListView, position);
				Toast.makeText(MainActivity.this, "Download complete", Toast.LENGTH_SHORT).show();
			} else if (intent.getAction().equals(DownloadService.ACTION_DOWNLOAD_ERROR)) {
				adapter.updateItem(imageListView, position);
				Toast.makeText(MainActivity.this, "Error while downloading", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, DownloadService.class);
		startService(intent);

		setContentView(R.layout.activity_main);
		downloadButton = (Button) findViewById(R.id.download_button);
		downloadButton.setOnClickListener(this);
		imageListView = (ListView) findViewById(R.id.image_list);
		adapter = new PreviewAdapter(this);
		disableUI();
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
		// TODO add changing text on button
		menu.add("Stop service");
		return true;
	}

	private void disableUI() {
		downloadButton.setEnabled(false);
	}

	private void enableUI() {
		downloadButton.setEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int order = item.getOrder();
		if (order == 0) {
			// TODO add stopping the service
			Log.d(LOG_TAG, "Click on start/stop service");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.download_button && bound) {
			downloadService.downloadImage(((EditText) findViewById(R.id.download_url)).getText().toString());
			adapter.updateData(downloadService.getDownloads());
		}
	}
}
