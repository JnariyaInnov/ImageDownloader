private package com.andriipanasiuk.imagedownloader.model;

import org.androidannotations.annotations.EBean;
import org.droidparts.adapter.cursor.EntityCursorAdapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.andriipanasiuk.imagedownloader.MainActivity;

@EBean
public class PreviewAdapter extends EntityCursorAdapter<DownloadInfo> {

	public PreviewAdapter(Context ctx) {
		super(ctx, DownloadInfo.class);
	}

	public void updateItem(ListView listView, long position, DownloadInfo info) {
		int first = listView.getFirstVisiblePosition();
		int last = listView.getLastVisiblePosition();
		for (int i = 0; i <= last - first; i++) {
			View child = listView.getChildAt(i);
			long id = (Long) child.getTag();
			if (id == info.id) {
				Log.d(MainActivity.LOG_TAG, first + " " + last + " " + (i + first) + " " + info.progress);
				((PreviewItem) child).updateItem(info);
			}
		}
	}

	@Override
	public void bindView(Context arg0, View convertView, DownloadInfo info) {
		((PreviewItem) convertView).updateItem(info);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return PreviewItem_.build(context);
	}

}
