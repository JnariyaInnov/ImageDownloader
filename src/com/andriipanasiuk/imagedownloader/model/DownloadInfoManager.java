package com.andriipanasiuk.imagedownloader.model;

import org.androidannotations.annotations.EBean;
import org.droidparts.persist.sql.EntityManager;

import android.content.Context;


@EBean
public class DownloadInfoManager extends EntityManager<DownloadInfo> {

	public DownloadInfoManager(Context ctx) {
		super(DownloadInfo.class, ctx);
	}

}