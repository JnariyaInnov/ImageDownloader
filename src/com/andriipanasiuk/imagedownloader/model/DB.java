package com.andriipanasiuk.imagedownloader.model;

import java.util.ArrayList;
import java.util.List;

public class DB {

	private static volatile DB instance;

	private DB() {
		// for defeating construction
	}

	private List<DownloadInfo> downloads = new ArrayList<DownloadInfo>();

	public synchronized int addDownload(DownloadInfo info) {
		downloads.add(info);
		return downloads.size() - 1;
	}

	public void clear() {
		downloads.clear();
	}

	public DownloadInfo get(int id) {
		return downloads.get(id);
	}

	public List<DownloadInfo> getDownloads() {
		return downloads;
	}

	public static DB getInstance() {
		DB localInstance = instance;
		if (localInstance == null) {
			synchronized (DB.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new DB();
				}
			}
		}
		return localInstance;
	}
}
