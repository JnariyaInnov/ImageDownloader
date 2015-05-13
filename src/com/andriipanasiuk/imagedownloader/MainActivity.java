package com.andriipanasiuk.imagedownloader;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;

import android.app.Service;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.andriipanasiuk.imagedownloader.model.DB;
import com.andriipanasiuk.imagedownloader.model.PreviewAdapter;
import com.andriipanasiuk.imagedownloader.service.DownloadService;
import com.andriipanasiuk.imagedownloader.service.DownloadService.DownloadBinder;
import com.squareup.picasso.Picasso;

@EActivity(value = R.layout.activity_main)
public class MainActivity extends ServiceActivity implements ServiceConnection {

	public static final String LOG_TAG = "ImageDownloader";
	private DownloadService downloadService;
	@ViewById(R.id.image_list)
	ListView imageListView;
	@ViewById(R.id.download_button)
	Button downloadButton;
	@ViewById(R.id.download_url)
	EditText urlEditText;
	@Bean
	PreviewAdapter adapter;

	private static final String[] IMAGE_URLS = new String[] { "http://edmullen.net/test/rc.jpg",
			"http://www.midiboutique.com/image/data/android-app-button.png",
			"https://thenypost.files.wordpress.com/2013/12/nasa-selfie.jpg",
			"https://reblogsocial.files.wordpress.com/2013/09/iiakkisefme9w1.jpg" };

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

	@Receiver(actions = DownloadService.ACTION_DOWNLOAD_PROGRESS)
	void onDownloadProgress(@Receiver.Extra(DownloadService.DOWNLOAD_ID_KEY) int position) {
		adapter.updateItem(imageListView, position);
	}

	@Receiver(actions = DownloadService.ACTION_DOWNLOAD_COMPLETE)
	void onDownloadComplete(@Receiver.Extra(DownloadService.DOWNLOAD_ID_KEY) int position) {
		adapter.updateItem(imageListView, position);
		Toast.makeText(MainActivity.this, R.string.download_complete, Toast.LENGTH_SHORT).show();
	}

	@Receiver(actions = DownloadService.ACTION_DOWNLOAD_ERROR)
	void onDownloadError(@Receiver.Extra(DownloadService.DOWNLOAD_ID_KEY) int position) {
		adapter.updateItem(imageListView, position);
		Toast.makeText(MainActivity.this, R.string.error_while_downloading, Toast.LENGTH_SHORT).show();
	}

	@Receiver(actions = DownloadService.ACTION_DOWNLOAD_CANCELLED)
	void onDownloadCancelled() {
		adapter.notifyDataSetChanged();
	}

	protected Class<? extends Service> getServiceClass() {
		return DownloadService.class;
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

		Picasso.with(this).setLoggingEnabled(true);

	}

	@AfterViews
	void init() {
		urlEditText.setText(IMAGE_URLS[1]);
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
