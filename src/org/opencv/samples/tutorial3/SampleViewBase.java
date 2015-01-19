package org.opencv.samples.tutorial3;

import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

public abstract class SampleViewBase extends SurfaceView implements
		SurfaceHolder.Callback, Runnable {
	private static final String TAG = "Sample::SurfaceView";

	public Camera mCamera;
	private SurfaceHolder mHolder;
	private int mFrameWidth;
	private int mFrameHeight;
	private byte[] mFrame;
	private boolean mThreadRun;
	private byte[] mBuffer;
	private byte[] YUVdata;

	public SampleViewBase(Context context) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);

		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	public int getFrameWidth() {
		return mFrameWidth;
	}

	public int getFrameHeight() {
		return mFrameHeight;
	}
	
	public void setFrameWidth(int width) {
		mFrameWidth = width;
	}

	public void setFrameHeight(int height) {
		mFrameHeight = height;
	}
	
	public void setYUVdata(byte[] yuv)
	{
		YUVdata = yuv;
	}
	
	public byte[] getYUVdata()
	{
		return YUVdata;
	}

	@SuppressLint("NewApi")
	public void setPreview() throws IOException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mCamera.setPreviewTexture(new SurfaceTexture(10));
		else
			mCamera.setPreviewDisplay(null);
	}

	public void surfaceChanged(SurfaceHolder _holder, int format, int width,
			int height) {
		Log.i(TAG, "surfaceCreated");
		if (mCamera != null) {
			mFrameWidth = width;
			mFrameHeight = height;

			Camera.Parameters params = mCamera.getParameters();
			List<Size> previewSizes = params.getSupportedPreviewSizes();
			List<Size> pictureSizes = params.getSupportedPictureSizes();
			Size previewSize = previewSizes.get(0);
			Size pictureSize = pictureSizes.get(0);
			int tempMinDiff = 0;
			int minDiff = 9999;

			for (Size s : previewSizes) {
				float currentHeight = s.height;

				tempMinDiff = (int) Math.abs(previewSizes.get(0).height / 2
						- currentHeight);

				if (tempMinDiff < minDiff) {
					minDiff = tempMinDiff;
					previewSize = s;
				}

			}

			tempMinDiff = 0;
			minDiff = 9999;

			for (Size s : pictureSizes) {
				float currentHeight = s.height;

				tempMinDiff = (int) Math.abs(pictureSizes.get(0).height / 5
						- currentHeight);

				if (tempMinDiff < minDiff) {
					minDiff = tempMinDiff;
					pictureSize = s;
				}

			}

			mFrameWidth = previewSize.width;
			mFrameHeight = previewSize.height;
			params.setPreviewSize(previewSize.width, previewSize.height);
			params.setPictureSize(pictureSize.width, pictureSize.height);
			params.setRotation(270);
			mCamera.setParameters(params);
			try {
				mCamera.setPreviewDisplay(getHolder());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// ////////////////////////////////////////////////////////

			// selecting optimal camera preview size

			List<String> FocusModes = params.getSupportedFocusModes();
			if (FocusModes
					.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			}
			
			
			mCamera.setParameters(params);

			/* Now allocate the buffer */
			params = mCamera.getParameters();
			int size = params.getPreviewSize().width
					* params.getPreviewSize().height;
			size = size
					* ImageFormat.getBitsPerPixel(params.getPreviewFormat())
					/ 8;
			mBuffer = new byte[size];
			/* The buffer where the current frame will be coppied */
			mFrame = new byte[size];
			mCamera.addCallbackBuffer(mBuffer);

			try {
				setPreview();
			} catch (IOException e) {
				Log.e(TAG,
						"mCamera.setPreviewDisplay/setPreviewTexture fails: "
								+ e);
			}

			/*
			 * Notify that the preview is about to be started and deliver
			 * preview size
			 */
			onPreviewStared(params.getPreviewSize().width,
					params.getPreviewSize().height);

			mCamera.setDisplayOrientation(90);
			/* Now we can start a preview */
			mCamera.startPreview();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		openFrontFacingCamera();
		mCamera.setDisplayOrientation(90);
		setParamsAndStartPreview();
		// mCamera.setDisplayOrientation(90);
		mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				synchronized (SampleViewBase.this) {
					mCamera.setDisplayOrientation(90);
					System.arraycopy(data, 0, mFrame, 0, data.length);
					SampleViewBase.this.notify();
				}
				camera.addCallbackBuffer(mBuffer);
			}
		});

		(new Thread(this)).start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed");
		mThreadRun = false;
		if (mCamera != null) {
			synchronized (this) {
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
		}
		onPreviewStopped();
	}

	public void openFrontFacingCamera() {
		releaseCameraAndPreview();
		int cameraCount = 0;
		mCamera = null;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				try {
					mCamera = Camera.open(camIdx);
					// isFrontCameraOpen = true;
				} catch (RuntimeException e) {
					Log.e("Changing cameras",
							"Camera failed to open: " + e.getLocalizedMessage());
					// isFrontCameraOpen = false;
				}
			}
		}
	}

	public void releaseCameraAndPreview() {

		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	public void setParamsAndStartPreview() {
		Camera.Parameters params = mCamera.getParameters();
		List<Size> previewSizes = params.getSupportedPreviewSizes();
		List<Size> pictureSizes = params.getSupportedPictureSizes();
		Size previewSize = previewSizes.get(0);
		Size pictureSize = pictureSizes.get(0);
		int tempMinDiff = 0;
		int minDiff = 9999;

		for (Size s : previewSizes) {
			float currentHeight = s.height;

			tempMinDiff = (int) Math.abs(previewSizes.get(0).height / 3
					- currentHeight);

			if (tempMinDiff < minDiff) {
				minDiff = tempMinDiff;
				previewSize = s;
			}

		}

		tempMinDiff = 0;
		minDiff = 9999;

		for (Size s : pictureSizes) {
			float currentHeight = s.height;

			tempMinDiff = (int) Math.abs(pictureSizes.get(0).height / 5
					- currentHeight);

			if (tempMinDiff < minDiff) {
				minDiff = tempMinDiff;
				pictureSize = s;
			}

		}

		params.setPreviewSize(previewSize.width, previewSize.height);
		params.setPictureSize(pictureSize.width, pictureSize.height);
		mCamera.setParameters(params);
		try {
			mCamera.setPreviewDisplay(getHolder());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// mCamera.setPreviewCallback(this);
		// mCamera.startPreview();
	}

	public void openPausedCamera() {
		// if (isFrontCameraOpen) {
		if (true) {
			openFrontFacingCamera();
			setParamsAndStartPreview();
		} else {

			// openBackFacingCamera();
			setParamsAndStartPreview();
		}
	}
	
	public void capture(Camera.PictureCallback jpegHandler) {
		mCamera.takePicture(null, null, jpegHandler);
	}
	public void switchCameras() {
		/*//if (//isFrontCameraOpen) {
			//openBackFacingCamera();
			//setParamsAndStartPreview();
		} else {
			openFrontFacingCamera();
			setParamsAndStartPreview();
		}*/
	}

	/*
	 * The bitmap returned by this method shall be owned by the child and
	 * released in onPreviewStopped()
	 */
	protected abstract Bitmap processFrame(byte[] data);

	/**
	 * This method is called when the preview process is beeing started. It is
	 * called before the first frame delivered and processFrame is called It is
	 * called with the width and height parameters of the preview process. It
	 * can be used to prepare the data needed during the frame processing.
	 * 
	 * @param previewWidth
	 *            - the width of the preview frames that will be delivered via
	 *            processFrame
	 * @param previewHeight
	 *            - the height of the preview frames that will be delivered via
	 *            processFrame
	 */
	protected abstract void onPreviewStared(int previewWidtd, int previewHeight);

	/**
	 * This method is called when preview is stopped. When this method is called
	 * the preview stopped and all the processing of frames already completed.
	 * If the Bitmap object returned via processFrame is cached - it is a good
	 * time to recycle it. Any other resourcses used during the preview can be
	 * released.
	 */
	protected abstract void onPreviewStopped();

	public void run() {
		mThreadRun = true;
		Log.i(TAG, "Starting processing thread");
		while (mThreadRun) {
			Bitmap bmp = null;

			synchronized (this) {
				try { 
					this.wait();
					mCamera.setDisplayOrientation(90);
					bmp = processFrame(mFrame);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if (bmp != null) {

				Canvas canvas = mHolder.lockCanvas();
				if (canvas != null) {
					Matrix matrix = new Matrix();
					matrix.postRotate(-90);
				
					//bmp = Bitmap.createBitmap(bmp, 0, 0, getFrameWidth(), getFrameHeight(), matrix, true);
					canvas.drawBitmap(bmp, 0, 0, null);
					mHolder.unlockCanvasAndPost(canvas);
				}

			}
		}
	}
}
