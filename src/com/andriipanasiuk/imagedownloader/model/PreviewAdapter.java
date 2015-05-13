private package com.andriipanasiuk.imagedownloader.model;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.andriipanasiuk.imagedownloader.MainActivity;

@EBean
public class PreviewAdapter extends BaseAdapter {

	private List<DownloadInfo> data;
	@RootContext
	Context context;

	public PreviewAdapter(Context context) {
		this.context = context;
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
		Log.d(MainActivity.LOG_TAG, "updateData " + data.size());
		notifyDataSetChanged();
	}

	public void updateItem(ListView listView, int position) {
		int first = listView.getFirstVisiblePosition();
		int last = listView.getLastVisiblePosition();
		DownloadInfo info = getItem(position);
		Log.d(MainActivity.LOG_TAG, first + " " + last + " " + position + " " + info.progress);
		if (position >= first && position <= last) {
			View convertView = listView.getChildAt(position - first);
			((PreviewItem) convertView).updateItem(info);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = PreviewItem_.build(context);
		}
		DownloadInfo info = getItem(position);
		((PreviewItem) convertView).updateItem(info);
		return convertView;
	}

}
