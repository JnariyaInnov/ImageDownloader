package com.andriipanasiuk.imagedownloader.model;

public class DownloadInfo {
	public String url;
	public String path;
	public int progress;
	public int downloadedBytes;
	public int allBytes;
	public State state;

	public static enum State {
		WAITING, PROCESS, COMPLETE, CANCELLED, ERROR
	}
}