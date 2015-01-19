package org.opencv.samples.tutorial3;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

//import android.util.Log;

class Sample3View extends SampleViewBase {

	private int mFrameSize;
	private Bitmap mBitmap;
	private int[] mRGBA;
	private boolean sizeSwitched = false;
	public static Bitmap bitmap;

	public Sample3View(Context context) {
		super(context);
		try {
			InputStream is = context.getResources().openRawResource(
					R.raw.haarcascade_frontalface_alt2);
			File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
			File cascadeFile = new File(cascadeDir,
					"haarcascade_frontalface_alt.xml");
			FileOutputStream os = new FileOutputStream(cascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();

			InputStream is2 = context.getResources().openRawResource(
					R.raw.muct76);
			File cascadeDir2 = context.getDir("muct76", Context.MODE_PRIVATE);
			File cascadeFile2 = new File(cascadeDir2, "muct76.model");
			FileOutputStream os2 = new FileOutputStream(cascadeFile2);
			/*
			 * Log.i("TAG", String.valueOf(cascadeFile2.length()));
			 * if(!cascadeFile2.canRead()){ Log.i("TAG", "yomenaiyo!!!"); }
			 * 
			 * is2.close();
			 */
			byte[] buffer2 = new byte[4096];
			int bytesRead2;
			while ((bytesRead2 = is2.read(buffer2)) != -1) {
				os2.write(buffer2, 0, bytesRead2);
			}
			is2.close();
			os2.close();

			// Log.i("TAG", "01Loaded cascade classifier from " +
			// cascadeFile.getAbsolutePath());
			readASMModel(cascadeFile2.getAbsolutePath(),
					cascadeFile.getAbsolutePath());
			// Log.i("TAG", "02Loaded cascade classifier from " +
			// cascadeFile2.getAbsolutePath());

			cascadeFile.delete();
			cascadeDir.delete();

			cascadeFile2.delete();
			cascadeDir2.delete();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPreviewStared(int previewWidtd, int previewHeight) {
		mFrameSize = previewWidtd * previewHeight;
		mRGBA = new int[mFrameSize];
		mBitmap = Bitmap.createBitmap(previewHeight, previewWidtd,
				Bitmap.Config.ARGB_8888);
	}

	@Override
	protected void onPreviewStopped() {
		if (mBitmap != null) {
			mBitmap.recycle();
			mBitmap = null;
		}
		mRGBA = null;

	}

	@Override
	protected Bitmap processFrame(byte[] data) {
		int[] rgba = mRGBA;

		byte[] rotatedData = new byte[data.length];
		/*
		 * for (int y = 0; y < getFrameHeight(); y++) { for (int x = 0; x <
		 * getFrameWidth(); x++) rotatedData[x * getFrameHeight() +
		 * getFrameHeight() - y - 1] = data[x + y * getFrameWidth()]; }
		 */

		rotatedData = rotateYUV420Degree90(data, getFrameWidth(),
				getFrameHeight());
		rotatedData = rotateYUV420Degree90(rotatedData, getFrameHeight(),
				getFrameWidth());
		rotatedData = rotateYUV420Degree90(rotatedData, getFrameWidth(),
				getFrameHeight());
		
		Bitmap bmap = getBitmapImageFromYUV(rotatedData, getFrameHeight(), getFrameWidth());
		bitmap = bmap;
		
		FindFeatures(bmap.getWidth(), bmap.getHeight(), rotatedData, rgba);

		Bitmap bmp = Bitmap.createBitmap(bmap.getWidth(), bmap.getHeight(),
				Bitmap.Config.ARGB_8888);;
		bmp.setPixels(rgba, 0/* offset */, bmap.getWidth() /* stride */, 0, 0,
				bmap.getWidth(), bmap.getHeight());
		flipBitmap(bmp);

		return bmp;
	}

	public static Bitmap getBitmapImageFromYUV(byte[] data, int width,
			int height) {
		YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height,
				null);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);
		byte[] jdata = baos.toByteArray();
		BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
		bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length,
				bitmapFatoryOptions);
		return bmp;
	}

	private byte[] rotateYUV420Degree90(byte[] data, int imageWidth,
			int imageHeight) {
		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
		// Rotate the Y luma
		int i = 0;
		for (int x = 0; x < imageWidth; x++) {
			for (int y = imageHeight - 1; y >= 0; y--) {
				yuv[i] = data[y * imageWidth + x];
				i++;
			}
		}
		// Rotate the U and V color components
		i = imageWidth * imageHeight * 3 / 2 - 1;
		for (int x = imageWidth - 1; x > 0; x = x - 2) {
			for (int y = 0; y < imageHeight / 2; y++) {
				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
				i--;
				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
						+ (x - 1)];
				i--;
			}
		}
		return yuv;
	}

	public void flipBitmap(Bitmap bmp)
	{
		Matrix matrix = new Matrix();
		matrix.preScale(-1.0f,  1.0f);
		bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);
	}
	public native void FindFeatures(int width, int height, byte yuv[],
			int[] rgba);

	public native void readASMModel(String c, String d);

	static {
		try {
			// System.loadLibrary("opencv_java"); // load opencv_java lib

			System.loadLibrary("native_sample");
		} catch (Exception e) {

		}
	}
}
