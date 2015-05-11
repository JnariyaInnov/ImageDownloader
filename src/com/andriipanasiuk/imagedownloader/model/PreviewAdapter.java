private package com.andriipanasiuk.imagedownloader.model;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.andriipanasiuk.imagedownloader.MainActivity;
import com.andriipanasiuk.imagedownloader.R;
import com.andriipanasiuk.imagedownloader.model.DownloadInfo.State;

public class PreviewAdapter extends BaseAdapter {

	private List<DownloadInfo> data;
	private Context context;
	private LruCache<String, Bitmap> memoryCache;

	public PreviewAdapter(Context context, LruCache<String, Bitmap> memoryCache) {
		this.context = context;
		this.memoryCache = memoryCache;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public DownloadInfo getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void updateData(List<DownloadInfo> data) {
		this.data = data;
		notifyDataSetChanged();
	}

	public void updateItem(ListView listView, int position) {
		int first = listView.getFirstVisiblePosition();
		int last = listView.getLastVisiblePosition();
		DownloadInfo info = getItem(position);
		Log.d(MainActivity.LOG_TAG, first + " " + last + " " + position + " " + info.progress);
		if (position < first || position > last) {
		} else {
			View convertView = listView.getChildAt(position - first);
			ViewHolder holder = (ViewHolder) convertView.getTag();
			updateItemInternal(holder, info);
		}
	}

	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.image_item, null);
			holder = new ViewHolder();
			holder.stateText = (TextView) convertView.findViewById(R.id.download_status_text);
			holder.previewImage = (ImageView) convertView.findViewById(R.id.preview_image);
			holder.progressBar = (ProgressBar) convertView.findViewById(R.id.download_progress);
			holder.progressText = (TextView) convertView.findViewById(R.id.download_progress_text);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		DownloadInfo info = getItem(position);
		updateItemInternal(holder, info);
		return convertView;
	}

	private void updateItemInternal(ViewHolder holder, DownloadInfo info) {
		if (info.state == State.PROCESS) {
			holder.stateText.setVisibility(View.GONE);
			holder.progressBar.setVisibility(View.VISIBLE);
			holder.progressText.setVisibility(View.VISIBLE);
			holder.progressBar.setProgress(info.progress);
			holder.previewImage.setImageResource(android.R.color.darker_gray);
			if (info.allBytes == 0) {
				holder.progressText.setText("");
			} else {
				holder.progressText.setText(bytesToUIString(info.downloadedBytes) + "/"
						+ bytesToUIString(info.allBytes));
			}
		} else {
			holder.stateText.setVisibility(View.VISIBLE);
			holder.progressBar.setVisibility(View.GONE);
			holder.progressText.setVisibility(View.GONE);
			if (info.state == State.CANCELLED) {
				holder.stateText.setText("Cancelled");
				holder.previewImage.setImageResource(android.R.color.darker_gray);
			} else if (info.state == State.COMPLETE) {
				holder.stateText.setText("Complete");
				Bitmap bitmap = memoryCache.get(info.path);
				if (bitmap == null) {
					holder.previewImage.setImageResource(android.R.color.darker_gray);
					new ImageLoader(holder.previewImage, memoryCache).execute(info.path);
				} else {
					holder.previewImage.setImageBitmap(bitmap);
				}
			} else if (info.state == State.ERROR) {
				holder.previewImage.setImageResource(android.R.color.darker_gray);
				holder.stateText.setText("Error");
			} else if (info.state == State.WAITING) {
				holder.previewImage.setImageResource(android.R.color.darker_gray);
				holder.stateText.setText("Waiting");
			}
		}
	}

	private String bytesToUIString(int bytes) {
		int kbytes = bytes / 1024;
		if (kbytes < 1024) {
			return kbytes + "KB";
		}
		int mbytes = kbytes / 1024;
		return mbytes + "," + (kbytes % 1024) * 100 / 1024 + "MB";
	}

	private static class ViewHolder {
		ImageView previewImage;
		ProgressBar progressBar;
		TextView progressText;
		TextView stateText;
	}
}
