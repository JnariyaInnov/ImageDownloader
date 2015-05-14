package com.andriipanasiuk.imagedownloader.model;

import org.droidparts.persist.sql.EntityManager;

import android.content.Context;


public class DownloadInfoManager extends EntityManager<DownloadInfo> {

	public DownloadInfoManager(Context ctx) {
		super(DownloadInfo.class, ctx);
	}

}