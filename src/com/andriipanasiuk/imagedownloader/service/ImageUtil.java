package com.andriipanasiuk.imagedownloader.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.andriipanasiuk.imagedownloader.MainActivity;

public class ImageUtil {

	public static Bitmap createScaled(File file, int width, int height) throws IOException {
		InputStream in = new FileInputStream(file);

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(in, null, options);
		in.close();
		in = null;

		int origWidth = options.outWidth;
		int origHeight = options.outHeight;

		double ratio = (double) origWidth / origHeight;
		int scaledWidth, scaledHeight;
		if (ratio > 1) {
			scaledWidth = width;
			scaledHeight = (int) (scaledWidth / ratio);
		} else {
			scaledHeight = height;
			scaledWidth = (int) (scaledHeight * ratio);
		}

		double resizeFactor = origWidth / scaledWidth;
		int inSampleSize = 1;
		while(inSampleSize < resizeFactor){
			inSampleSize *= 2;
		}
		inSampleSize /= 2;
		Log.d(MainActivity.LOG_TAG, "inSample: " + inSampleSize);
		in = new FileInputStream(file);
		options = new BitmapFactory.Options();
		options.inSampleSize = inSampleSize;
		Bitmap roughBitmap = BitmapFactory.decodeStream(in, null, options);

		Bitmap resizedBitmap = Bitmap.createScaledBitmap(roughBitmap, scaledWidth, scaledHeight, false);
		roughBitmap.recycle();
		return resizedBitmap;
	}
}
