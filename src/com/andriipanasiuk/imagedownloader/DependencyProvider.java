package com.andriipanasiuk.imagedownloader;

import org.droidparts.AbstractDependencyProvider;
import org.droidparts.persist.sql.AbstractDBOpenHelper;

import com.andriipanasiuk.imagedownloader.model.DBHelper;
import com.andriipanasiuk.imagedownloader.model.DownloadInfoManager;

import android.content.Context;

public class DependencyProvider extends AbstractDependencyProvider {

	public DependencyProvider(Context ctx) {
		super(ctx);
	}

	public DownloadInfoManager getDownloadInfoManager(Context c) {
		return new DownloadInfoManager(c);
	}

	@Override
	public AbstractDBOpenHelper getDBOpenHelper() {
		return new DBHelper(getContext());
	}

}
