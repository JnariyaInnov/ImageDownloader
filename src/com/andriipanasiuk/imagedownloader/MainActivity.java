package com.andriipanasiuk.imagedownloader;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.andriipanasiuk.imagedownloader.model.DB;
import com.andriipanasiuk.imagedownloader.model.PreviewAdapter;
import com.andriipanasiuk.imagedownloader.service.DownloadService;
import com.andriipanasiuk.imagedownloader.service.DownloadService.DownloadBinder;

public class MainActivity extends ServiceActivity implements OnClickListener, ServiceConnection {

	public static final String LOG_TAG = "ImageDownloader";
	private DownloadService downloadService;
	private ProgressReceiver receiver;
	private ListView imageListView;
	private Button downloadButton;
	private EditText urlEditText;
	private PreviewAdapter adapter;

	private static final String[] IMAGE_URLS = new String[] {
			"http://www.tsquirrel.com/_data/photos/2014/11/1287__nasa_after-a-keen-searched-of-nasa-pictures-hd-wallpapers-inn-.jpg",
			"http://www.midiboutique.com/image/data/android-app-button.png",
			"https://thenypost.files.wordpress.com/2013/12/nasa-selfie.jpg",
			"https://reblogsocial.files.wordpress.com/2013/09/iiakkisefme9w1.jpg" };
	private LruCache<String, Bitmap> memoryCache;

	@Override
	public void onServiceDisconnected(ComponentName name) {
		super.onServiceDisconnected(name);
		disableUI();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		super.onServiceConnected(name, service);
		Log.d(LOG_TAG, "onServiceConnected");
		downloadService = ((DownloadBinder) service).getService();
		enableUI();
	}

	private class ProgressReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int position = intent.getIntExtra(DownloadService.DOWNLOAD_ID_KEY, -1);
			String action = intent.getAction();
			if (action.equals(DownloadService.ACTION_DOWNLOAD_PROGRESS)) {
				adapter.updateItem(imageListView, position);
			} else if (action.equals(DownloadService.ACTION_DOWNLOAD_COMPLETE)) {
				adapter.updateItem(imageListView, position);
				Toast.makeText(MainActivity.this, R.string.download_complete, Toast.LENGTH_SHORT).show();
			} else if (action.equals(DownloadService.ACTION_DOWNLOAD_ERROR)) {
				adapter.updateItem(imageListView, position);
				Toast.makeText(MainActivity.this, R.string.error_while_downloading, Toast.LENGTH_SHORT).show();
			} else if (action.equals(DownloadService.ACTION_DOWNLOAD_CANCELLED)) {
				adapter.notifyDataSetChanged();
			}
		}
	}

	protected Class<? extends Service> getServiceClass() {
		return DownloadService.class;
	}

	protected void registerReceiver() {
		receiver = new ProgressReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_DOWNLOAD_PROGRESS);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_COMPLETE);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_ERROR);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_CANCELLED);
		registerReceiver(receiver, filter);
	}

	protected void unregisterReceiver() {
		unregisterReceiver(receiver);
	}

	@Override
	protected void stopService(boolean immediately) {
		if (immediately) {
			downloadService.stopNow();
		} else {
			downloadService.stop();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(getFragmentManager());
		memoryCache = retainFragment.mRetainedCache;
		if (memoryCache == null) {
			int cacheSize = 4 * 1024 * 1024; // 4MiB
			memoryCache = new LruCache<String, Bitmap>(cacheSize) {
				protected int sizeOf(String key, Bitmap value) {
					return value.getByteCount();

				}
			};
			retainFragment.mRetainedCache = memoryCache;
		}

		setContentView(R.layout.activity_main);
		downloadButton = (Button) findViewById(R.id.download_button);
		urlEditText = (EditText) findViewById(R.id.download_url);
		urlEditText.setText(IMAGE_URLS[1]);
		downloadButton.setOnClickListener(this);
		imageListView = (ListView) findViewById(R.id.image_list);
		adapter = new PreviewAdapter(this, memoryCache);
		adapter.updateData(DB.getInstance().getDownloads());
		imageListView.setAdapter(adapter);
	}

	private void disableUI() {
		downloadButton.setEnabled(false);
	}

	private void enableUI() {
		downloadButton.setEnabled(true);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.download_button) {
			if (isServiceBound()) {
				String url = urlEditText.getText().toString();
				boolean result = downloadService.downloadImage(url);
				if (result) {
					adapter.notifyDataSetChanged();
				} else {
					Toast.makeText(this, R.string.service_is_stopped, Toast.LENGTH_SHORT).show();
				}
			} else if (isServiceStopped()) {
				Toast.makeText(this, R.string.service_is_stopped, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, R.string.service_is_starting, Toast.LENGTH_SHORT).show();
			}
		}
	}

}
