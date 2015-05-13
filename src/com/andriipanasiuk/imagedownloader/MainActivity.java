package com.andriipanasiuk.imagedownloader;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.andriipanasiuk.imagedownloader.model.DB;
import com.andriipanasiuk.imagedownloader.model.PreviewAdapter;
import com.andriipanasiuk.imagedownloader.service.DownloadService;
import com.andriipanasiuk.imagedownloader.service.DownloadService.DownloadBinder;

@EActivity
public class MainActivity extends ServiceActivity implements ServiceConnection {

	public static final String LOG_TAG = "ImageDownloader";
	private DownloadService downloadService;
	private ProgressReceiver receiver;
	@ViewById(R.id.image_list)
	ListView imageListView;
	@ViewById(R.id.download_button)
	Button downloadButton;
	@ViewById(R.id.download_url)
	EditText urlEditText;
	private PreviewAdapter adapter;

	private static final String[] IMAGE_URLS = new String[] { "http://edmullen.net/test/rc.jpg",
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
		urlEditText.setText(IMAGE_URLS[0]);
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

	@Click(R.id.download_button)
	void onDownloadClick(View v) {
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
