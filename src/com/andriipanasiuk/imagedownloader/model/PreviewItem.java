package com.andriipanasiuk.imagedownloader.model;

import java.io.File;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.andriipanasiuk.imagedownloader.R;
import com.andriipanasiuk.imagedownloader.model.DownloadInfo.State;
import com.squareup.picasso.Picasso;

@EViewGroup(R.layout.image_item)
public class PreviewItem extends LinearLayout {

	public PreviewItem(Context context) {
		super(context);
	}

	private String bytesToUIString(int bytes) {
		int kbytes = bytes / 1024;
		if (kbytes < 1024) {
			return kbytes + "KB";
		}
		int mbytes = kbytes / 1024;
		return mbytes + "," + (kbytes % 1024) * 100 / 1024 + "MB";
	}

	@ViewById(R.id.preview_image)
	ImageView previewImage;
	@ViewById(R.id.download_progress)
	ProgressBar progressBar;
	@ViewById(R.id.download_progress_text)
	TextView progressText;
	@ViewById(R.id.download_status_text)
	TextView stateText;

	void updateItem(DownloadInfo info) {
		if (info.state != State.COMPLETE) {
			previewImage.setImageResource(android.R.color.darker_gray);
		}
		if (info.state == State.PROCESS) {
			stateText.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			progressText.setVisibility(View.VISIBLE);
			progressBar.setProgress(info.progress);
			if (info.allBytes == 0) {
				progressText.setText("");
			} else {
				progressText.setText(bytesToUIString(info.downloadedBytes) + "/" + bytesToUIString(info.allBytes));
			}
		} else {
			stateText.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			progressText.setVisibility(View.GONE);
			if (info.state == State.CANCELLED) {
				stateText.setText(R.string.cancelled);
			} else if (info.state == State.COMPLETE) {
				stateText.setText(R.string.complete);
				Picasso.with(getContext()).load(new File(info.path)).fit().centerInside()
						.placeholder(android.R.color.darker_gray).into(previewImage);
			} else if (info.state == State.ERROR) {
				stateText.setText(R.string.error);
			} else if (info.state == State.WAITING) {
				stateText.setText(R.string.waiting);
			}
		}
	}

}