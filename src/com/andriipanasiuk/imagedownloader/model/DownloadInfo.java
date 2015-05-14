package com.andriipanasiuk.imagedownloader.model;

import org.droidparts.annotation.sql.Column;
import org.droidparts.annotation.sql.Table;
import org.droidparts.model.Entity;

@Table(name = "DownloadInfo")
public class DownloadInfo extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5452387666871759802L;
	@Column(name = "url")
	public String url;
	@Column(name = "path")
	public String path = "";
	@Column(name = "progress")
	public int progress;
	@Column(name = "downloadedBytes")
	public int downloadedBytes;
	@Column(name = "allBytes")
	public int allBytes;
	@Column(name = "state")
	public State state;

	public static enum State {
		WAITING, PROCESS, COMPLETE, CANCELLED, ERROR
	}
}