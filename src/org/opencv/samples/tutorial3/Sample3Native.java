package org.opencv.samples.tutorial3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Sample3Native extends Activity implements OnTouchListener,
		OnSeekBarChangeListener, Runnable {
	private static final String TAG = "Sample::Activity";

	private static final int CAMERA_REQUEST = 1888;
	private static final int CAMERA_PIC_REQUEST1 = 0;
	private Uri picUri;
	private String filePath = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + File.separator + "photo.jpg";
	private File file;
	private int x;
	private int y;
	ImageView imageView;
	static int[] fpx = null;
	static int[] fpy = null;
	private static Bitmap rotated = null;
	Button button = null;
	Handler handle = null;
	private static final int UPDATE_CANVAS = 1;
	static ImageView zoomView = null;
	LinearLayout linearLayout = null;
	TextView description = null;
	private int stringCounter = 0;
	private static float xCoor = 0;
	private static float yCoor = 0;
	static Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static int drawLines = 0;
	private Sample3View cameraView;
	private ImageView imageResult;
	public static FrameLayout frameLayoutCamera;
	private FrameLayout frameLayoutZoom;
	private boolean takePicture = true;
	private Bitmap image = null;
	private static int zoomedX, zoomedY;
	private static float scaledX, scaledY;
	private Bitmap scaledRotated = null;
	private GLSurfaceView mGLView;
	private boolean cameraInFront = false;
	private boolean imageViewInFront = false;
	private boolean zoomViewInFront = false;

	private PointF midpoint;
	public static FaceDetector.Face[] faces;
	private Mat srcMat;
	private Mat dstMat;

	private CameraBridgeViewBase mOpenCvCameraView;
	private SeekBar seekBar;
	private float threshold = 0;
	private ImageView binaryImage;
	private Mat contourMat;
	private ImageView contourImage;
	private TextView thresholdValue;
	private boolean onResumeFirstCall = true;
	private TextView faceDetected;
	public Point leftEye;
	public Point rightEye;
	private Point rightPartOfNose;
	private Point leftPartOfNose;
	private Point rightPartOfMouth;
	private Point leftPartOfMouth;
	private int numberOfFaceDetected;
	FaceDetector.Face[] myFace;
	private Paint paint2;
	private Bitmap contourBitmap;
	Handler mHandler;
	private Semaphore mutex;
	private boolean isImageGrayscale;
	Point midPointContour;
	List<MatOfPoint> contours;
	Point hairLine;
	Point[] nose;
	Point[] mouth;
	Point chin;
	Point leftSide;
	Point rightSide;
	private float eyeDistance;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);

		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this,
				mLoaderCallback);
		setContentView(R.layout.camera);

		paint.setColor(Color.BLUE);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5);

		frameLayoutCamera = (FrameLayout) findViewById(R.id.frameLayout1);
		frameLayoutZoom = (FrameLayout) findViewById(R.id.frameLayout2);
		linearLayout = (LinearLayout) findViewById(R.id.linear_layout);
		zoomView = (ImageView) findViewById(R.id.zoom_view);
		binaryImage = (ImageView) findViewById(R.id.binary_image);
		contourImage = (ImageView) findViewById(R.id.contoured_image);
		thresholdValue = (TextView) findViewById(R.id.threshold_value);
		faceDetected = (TextView) findViewById(R.id.facedetected);

		cameraInFront = true;

		paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint2.setColor(Color.BLUE);
		paint2.setStrokeWidth(5);
		paint2.setStyle(Paint.Style.STROKE);
		
		eyeDistance = 0;

		try {
			if (isCameraInFront()) {
				setUpCamera();
			}
		} catch (Exception e) {
			Log.d("Camera", "Error starting camera preview: " + e.getMessage());
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		if (cameraView.mCamera != null) {
			cameraView.mCamera.setPreviewCallback(null);
			cameraView.getHolder().removeCallback(cameraView);
			cameraView.releaseCameraAndPreview();
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this,
				mLoaderCallback);
		if (isCameraInFront() && !onResumeFirstCall) {
			cameraView.openPausedCamera();
		}
		onResumeFirstCall = false;
	}

	public void setUpCamera() {
		cameraView = new Sample3View(this);
		imageView = new ImageView(getApplicationContext());
		imageView.setOnTouchListener(this);

		imageView.setBackgroundColor(Color.WHITE);

		linearLayout.setBackgroundColor(Color.WHITE);

		frameLayoutCamera.addView(imageView);
		frameLayoutCamera.addView(cameraView);

		frameLayoutCamera.bringChildToFront(cameraView);
		cameraInFront = true;
		imageViewInFront = false;
		zoomViewInFront = false;

		frameLayoutCamera.setOnTouchListener(this);
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(this);

		startFeatureDetection();
	}

	public void startFeatureDetection() {

		isImageGrayscale = false;
		mutex = new Semaphore(1);
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				faceDetected.setText("" + wasFaceDetected() +"/n"
						 + eyeDistance);

				if (wasFaceDetected()) {
					Canvas canvas = new Canvas(contourBitmap);

					canvas.drawCircle((float) midPointContour.x,
							(float) midPointContour.y, 3, paint);
					if (hairLine != null) {
						canvas.drawCircle((float) hairLine.x,
								(float) hairLine.y, 5, paint2);
						contourImage.invalidate();
					}
					if (nose != null) {
						canvas.drawCircle((float) nose[0].x, (float) nose[0].y,
								5, paint2);
						contourImage.invalidate();

						canvas.drawCircle((float) nose[1].x, (float) nose[1].y,
								5, paint2);
						contourImage.invalidate();
					}
					if (mouth != null) {
						canvas.drawCircle((float) mouth[0].x,
								(float) mouth[0].y, 5, paint2);
						contourImage.invalidate();
						canvas.drawCircle((float) mouth[1].x,
								(float) mouth[1].y, 5, paint2);
						contourImage.invalidate();
					}
					if (chin != null) {
						canvas.drawCircle((float) chin.x, (float) chin.y, 5,
								paint2);
						contourImage.invalidate();
					}
					if (leftSide != null) {
						canvas.drawCircle((float) leftSide.x,
								(float) leftSide.y, 5, paint2);
						contourImage.invalidate();
					}
					if (rightSide != null) {
						canvas.drawCircle((float) rightSide.x,
								(float) rightSide.y, 5, paint2);
						contourImage.invalidate();
					}

					Canvas canvasImage = new Canvas(contourBitmap);
					if (leftEye != null && rightEye != null) {
						canvasImage.drawCircle((float) leftEye.x,
								(float) leftEye.y, 1, paint);
						canvasImage.drawCircle((float) rightEye.x,
								(float) rightEye.y, 1, paint);
					}
				}

				if (isImageGrayscale == true)
					binaryImage.setImageBitmap(contourBitmap);
				mutex.release();

			}
		};

		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Starting processing thread");
				while (true) {

					image = Sample3View.bitmap;
					isImageGrayscale = false;

					if (image != null) {

						Camera.Size cDimensions = cameraView.mCamera
								.getParameters().getPreviewSize();
						int width = cDimensions.width;
						int height = cDimensions.height;

						midpoint = new PointF();

						int imageWidth = image.getWidth();
						int imageHeight = image.getHeight();
						myFace = new FaceDetector.Face[5];
						FaceDetector myFaceDetect = new FaceDetector(
								imageWidth, imageHeight, 5);

						numberOfFaceDetected = myFaceDetect.findFaces(image,
								myFace);
						

						if (wasFaceDetected())
						{
							myFace[0].getMidPoint(midpoint);
							eyeDistance = myFace[0].eyesDistance();
						}

						// image = Binarize.toGrayscale(image);

						setUpAfterPicture();
						// setFace();

						// rotated = image.copy(image.getConfig(),
						// image.isMutable());

						width = image.getWidth();
						height = image.getHeight();

						srcMat = new Mat(width, height, CvType.CV_8UC1,
								new Scalar(0));
						dstMat = new Mat(width, height, CvType.CV_8UC1,
								new Scalar(0));
						contourMat = new Mat(width, height, CvType.CV_8UC1,
								new Scalar(0));

						Utils.bitmapToMat(image, srcMat);
						/*
						 * Imgproc.threshold(srcMat, dstMat, threshold, 255,
						 * Imgproc.THRESH_BINARY_INV);
						 */

						// Imgproc.equalizeHist(srcMat, dstMat);

						// for (int i = 0; i < 5; i++)
						// toon(srcMat, srcMat);

						Imgproc.GaussianBlur(srcMat, srcMat, new Size(3, 3), 0,
								0, Imgproc.BORDER_DEFAULT);

						Imgproc.cvtColor(srcMat, dstMat, Imgproc.COLOR_BGR2GRAY);
						isImageGrayscale = true;

						Mat grad_x = new Mat(width, height, CvType.CV_8UC1,
								new Scalar(0));
						Mat grad_y = new Mat(width, height, CvType.CV_8UC1,
								new Scalar(0));
						Mat abs_grad_x = new Mat(width, height, CvType.CV_8UC1,
								new Scalar(0));
						Mat abs_grad_y = new Mat(width, height, CvType.CV_8UC1,
								new Scalar(0));

						Size size1 = abs_grad_x.size();
						Size size2 = abs_grad_y.size();

						Mat grad = new Mat(width, height, CvType.CV_8UC1,
								new Scalar(0));

						int scale = 1;
						int delta = 0;
						int ddepth = 3;

						Imgproc.Scharr(dstMat, grad_x, ddepth, 1, 0, scale,
								delta, Imgproc.BORDER_DEFAULT);
						Core.convertScaleAbs(grad_x, abs_grad_x);

						size1 = abs_grad_x.size();

						Imgproc.Scharr(dstMat, grad_y, ddepth, 0, 1, scale,
								delta, Imgproc.BORDER_DEFAULT);
						Core.convertScaleAbs(grad_y, abs_grad_y);

						size2 = abs_grad_y.size();

						Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 0,
								dstMat);

						Imgproc.GaussianBlur(dstMat, dstMat, new Size(3, 3), 0,
								0, Imgproc.BORDER_DEFAULT);

						int erosion_elem = 0;
						int erosion_size = 1;
						int dilation_elem = 0;
						int dilation_size = 2;
						int max_elem = 2;
						int max_kernel_size = 21;

						/*
						 * Mat element = Imgproc.getStructuringElement(
						 * Imgproc.MORPH_RECT, new Size( 2 * dilation_size + 1,
						 * 2 * dilation_size + 1), new Point( dilation_size,
						 * dilation_size)); Imgproc.dilate(dstMat, dstMat,
						 * element);
						 */

						Mat element = Imgproc.getStructuringElement(
								Imgproc.MORPH_RECT, new Size(
										2 * erosion_size + 1,
										2 * erosion_size + 1), new Point(
										erosion_size, erosion_size));
						Imgproc.erode(dstMat, dstMat, element);

						Utils.matToBitmap(dstMat, image);

						outlineFiltered();

						/*
						 * canvas.drawCircle((float) largestXPoint.x, (float)
						 * largestXPoint.y, 3, paint); canvas.drawCircle((float)
						 * smallestXPoint.x, (float) smallestXPoint.y, 3,
						 * paint); canvas.drawCircle((float) largestYPoint.x,
						 * (float) largestYPoint.y, 3, paint);
						 * canvas.drawCircle((float) smallestYPoint.x, (float)
						 * smallestYPoint.y, 3, paint);
						 */

						if (wasFaceDetected()) {
							hairLine = findHairline(contours);
							nose = findNose(contours);
							mouth = findMouth(contours);
							chin = findChin(contours);
							leftSide = findLeftSide(contours);
							rightSide = findRightSide(contours);
						}

						Message msg = new Message();
						String textTochange = "" + wasFaceDetected();
						msg.obj = textTochange;
						mHandler.sendMessage(msg);

						try {
							mutex.acquire();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						/*
						 * frameLayoutCamera.bringChildToFront(imageView);
						 * cameraInFront = false; imageViewInFront = true;
						 * zoomViewInFront = false;
						 * 
						 * takePicture = false; imageView.invalidate();
						 */
					} else {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			}
		}.start();

	}

	public void captureHandler(View v) {
		if (takePicture) {
			cameraView.capture(jpegHandler);

		} else {
			takePicture = true;
			frameLayoutCamera.bringChildToFront(cameraView);
			cameraInFront = true;
			imageViewInFront = false;
			zoomViewInFront = false;
			imageView.setImageBitmap(null);
		}
	}

	public Camera.PictureCallback jpegHandler = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			Camera.Size size = cameraView.mCamera.getParameters()
					.getPictureSize();

			midpoint = new PointF();

			BitmapFactory.Options BitmapFactoryOptionsbfo = new BitmapFactory.Options();
			BitmapFactoryOptionsbfo.inPreferredConfig = Bitmap.Config.RGB_565;
			image = BitmapFactory.decodeByteArray(data, 0, data.length,
					BitmapFactoryOptionsbfo);

			setUpAfterPicture();

			int imageWidth = image.getWidth();
			int imageHeight = image.getHeight();
			myFace = new FaceDetector.Face[5];
			FaceDetector myFaceDetect = new FaceDetector(imageWidth,
					imageHeight, 5);

			numberOfFaceDetected = myFaceDetect.findFaces(image, myFace);

			if (wasFaceDetected())
				myFace[0].getMidPoint(midpoint);

			// image = Binarize.toGrayscale(image);

			// setFace();
			faceDetected.setText("" + wasFaceDetected());

			rotated = image.copy(image.getConfig(), image.isMutable());

			srcMat = new Mat(cameraView.mCamera.getParameters()
					.getPictureSize().width, cameraView.mCamera.getParameters()
					.getPictureSize().height, CvType.CV_8UC3, new Scalar(0));
			dstMat = new Mat(cameraView.mCamera.getParameters()
					.getPictureSize().width, cameraView.mCamera.getParameters()
					.getPictureSize().height, CvType.CV_8UC3, new Scalar(0));
			contourMat = new Mat(cameraView.mCamera.getParameters()
					.getPictureSize().width, cameraView.mCamera.getParameters()
					.getPictureSize().height, CvType.CV_8UC1, new Scalar(0));

			Utils.bitmapToMat(rotated, srcMat);
			/*
			 * Imgproc.threshold(srcMat, dstMat, threshold, 255,
			 * Imgproc.THRESH_BINARY_INV);
			 */

			// Imgproc.equalizeHist(srcMat, dstMat);

			// for (int i = 0; i < 5; i++)
			// toon(srcMat, srcMat);

			// Imgproc.GaussianBlur(srcMat, srcMat, new Size(3, 3), 0, 0,
			// Imgproc.BORDER_DEFAULT);

			/* Imgproc.cvtColor(dstMat, dstMat, Imgproc.COLOR_BGR2GRAY); */

			Mat grad_x = new Mat(cameraView.mCamera.getParameters()
					.getPictureSize().width, cameraView.mCamera.getParameters()
					.getPictureSize().height, CvType.CV_8UC1, new Scalar(0));
			Mat grad_y = new Mat(cameraView.mCamera.getParameters()
					.getPictureSize().width, cameraView.mCamera.getParameters()
					.getPictureSize().height, CvType.CV_8UC1, new Scalar(0));
			Mat abs_grad_x = new Mat(cameraView.mCamera.getParameters()
					.getPictureSize().width, cameraView.mCamera.getParameters()
					.getPictureSize().height, CvType.CV_8UC1, new Scalar(0));
			Mat abs_grad_y = new Mat(cameraView.mCamera.getParameters()
					.getPictureSize().width, cameraView.mCamera.getParameters()
					.getPictureSize().height, CvType.CV_8UC1, new Scalar(0));
			Mat grad = new Mat(cameraView.mCamera.getParameters()
					.getPictureSize().width, cameraView.mCamera.getParameters()
					.getPictureSize().height, CvType.CV_8UC1, new Scalar(0));

			int scale = 1;
			int delta = 0;
			int ddepth = 3;

			Imgproc.Scharr(dstMat, grad_x, ddepth, 1, 0, scale, delta,
					Imgproc.BORDER_DEFAULT);
			Core.convertScaleAbs(grad_x, abs_grad_x);

			Imgproc.Scharr(dstMat, grad_x, ddepth, 0, 1, scale, delta,
					Imgproc.BORDER_DEFAULT);
			Core.convertScaleAbs(grad_y, abs_grad_y);

			Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 0, dstMat);

			int erosion_elem = 0;
			int erosion_size = 1;
			int dilation_elem = 0;
			int dilation_size = 2;
			int max_elem = 2;
			int max_kernel_size = 21;

			/*
			 * Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
			 * new Size(2 * dilation_size + 1, 2 * dilation_size + 1), new
			 * Point(dilation_size, dilation_size)); Imgproc.dilate(dstMat,
			 * dstMat, element);
			 * 
			 * element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new
			 * Size(2 * erosion_size + 1, 2 * erosion_size + 1), new
			 * Point(erosion_size, erosion_size)); Imgproc.erode(dstMat, dstMat,
			 * element);
			 */

			Utils.matToBitmap(dstMat, rotated);

			binaryImage.setImageBitmap(rotated);

			/*
			 * frameLayoutCamera.bringChildToFront(imageView); cameraInFront =
			 * false; imageViewInFront = true; zoomViewInFront = false;
			 * 
			 * takePicture = false; imageView.invalidate();
			 */
		}
	};

	public void setUpAfterPicture() {
		// linearLayout.removeView(button);

		// Bitmap aFace = image.copy(Bitmap.Config.RGB_565, true);
		// image.recycle();
		float density = getResources().getDisplayMetrics().density;

		Matrix matrix = new Matrix();

		// if (cameraView.isFrontCameraOpen()) {
		if (true) {
			matrix.postRotate(-90);
		} else {
			matrix.postRotate(90);
		}

		// rotated = Bitmap.createBitmap(image, 0, 0, (int) (image.getWidth()),
		// (int) (image.getHeight()), matrix, true);
		// image.recycle();

		scaledRotated = Bitmap.createScaledBitmap(image,
				(int) frameLayoutCamera.getWidth() * 4,
				(int) frameLayoutCamera.getHeight() * 4, true);

		// Now bitmap.getWidth() == 6

		// imageView.setImageBitmap(image)

	}

	public void incrementCounter(View v) {
		if (stringCounter >= 0 && stringCounter < 14) {

			stringCounter++;
			updateDesc(updateString());
		}

		else if (stringCounter > 13)
			calculateAesthetics();
		imageView.invalidate();
	}

	public void updateDesc(String newString) {
		description.setText(newString);
	}

	public void calculateAesthetics() {
		double a, b, c, d, e, f, g, h, i, j, k, l;

		double aes1, aes2, aes3, aes4, aes5, aes6, aes7;

		drawLines = 1;

		imageView.invalidate();

	}

	public String updateString() {
		String newString = "";

		switch (stringCounter) {
		case 0:
			newString = "Place dot at the top of the head.";
			break;
		case 1:
			newString = "Place dot at your chin.";
			break;
		case 2:
			newString = "Place dot at your right pupil.";
			break;
		case 3:
			newString = "Place dot at your nosetip .";
			break;
		case 4:
			newString = "Place dot at the center of your lips.";
			break;
		case 5:
			newString = "Place dot at the right most part of the nose.";
			break;
		case 6:
			newString = "Place dot at the left most part of the nose.";
			break;
		case 7:
			newString = "Place dot at the outside part of your right eye.";
			break;
		case 8:
			newString = "Place dot at the outside part of your left eye.";
			break;
		case 9:
			newString = "Place dot at the right most part of your face.";
			break;
		case 10:
			newString = "Place dot at the left most part of your face.";
			break;
		case 11:
			newString = "Place dot at your hairline.";
			break;
		case 12:
			newString = "Place dot at the right of your lips.";
			break;
		case 13:
			newString = "Place dot at the left of your lips.";
			break;
		}

		return newString;
	}

	public void setFace() {
		FaceDetector fd;
		faces = new FaceDetector.Face[5];
		midpoint = new PointF();

		int count = 0;

		try {
			fd = new FaceDetector(rotated.getWidth(), rotated.getHeight(), 1);
			count = fd.findFaces(rotated, faces);
		} catch (Exception e) {
			Log.e("nothing", "setFace(): " + e.toString());
			return;
		}

		// check if we detect any faces
		if (count > 0) {
			fpx = new int[count];
			fpy = new int[count];

			for (int i = 0; i < count; i++) {
				try {
					faces[i].confidence();
					faces[i].getMidPoint(midpoint);

					fpx[i] = (int) (midpoint.x);
					fpy[i] = (int) (midpoint.y);
				} catch (Exception e) {
					Log.e("nothing",
							"setFace(): face " + i + ": " + e.toString());
				}
			}
		} else {
			fpx = new int[0];
			fpy = new int[0];
		}
	}

	public void zoom(float x, float y, Bitmap pic) {
		zoomView = (ImageView) findViewById(R.id.zoom_view);
		float density = getResources().getDisplayMetrics().density;
		zoomedX = (int) ((x) * 4) - 50;
		zoomedY = (int) ((y) * 4) - 50;
		int facePhotoWidth = frameLayoutCamera.getWidth();
		int facePhotoHeight = frameLayoutCamera.getHeight();
		scaledX = x;
		scaledY = y;

		if (((int) zoomedX) >= 0 && ((int) zoomedY) >= 0
				&& ((int) zoomedY) + 101 <= facePhotoHeight * 4 // i think this
																// needs
																// to be
																// imageView.getHeight/Width()
				&& ((int) zoomedX) + 101 <= facePhotoWidth * 4) {

			/*
			 * Bitmap bitmap = Bitmap.createBitmap(1000, 1000,
			 * Bitmap.Config.RGB_565); Canvas canvas1 = new Canvas(bitmap);
			 * zoomView.draw(canvas1);
			 */

			// Bitmap bmp = createCroppedBitmap(pic, zoomedY, zoomedX, 76, 76);
			Bitmap bmp2 = Bitmap.createBitmap(pic, (int) (zoomedX),
					(int) (zoomedY), 100, 100);

			Bitmap mutableBitmap = bmp2.copy(Bitmap.Config.ARGB_8888, true);
			bmp2.recycle();

			zoomView.setImageBitmap(mutableBitmap);
			Canvas canvas = new Canvas(mutableBitmap);
			zoomView.draw(canvas);
			// canvas.drawCircle(50, 50, 2, imageView.getCP().getPaint());

		}

	}

	@Override
	public boolean onTouch(View v, MotionEvent e) {
		// TODO Auto-generated method stub

		if (v.getId() == R.id.frameLayout1) {

			Log.d("TouchEvent", "Touch Event was called");

			xCoor = e.getX();
			yCoor = e.getY();

			if (isCameraInFront()) {
				captureHandler(frameLayoutCamera);
				return false;
			}

		}
		return false;
	}

	public boolean isCameraInFront() {
		return cameraInFront;
	}

	public boolean isZoomViewInFront() {
		return zoomViewInFront;
	}

	public boolean isImageViewInFront() {
		return imageViewInFront;
	}

	public void updateThreshold() {

		threshold = (float) (seekBar.getProgress()) + 50;
		thresholdValue.setText("" + threshold);

		/*
		 * Utils.bitmapToMat(image, srcMat); Imgproc.threshold(srcMat, dstMat,
		 * threshold, 255, Imgproc.THRESH_BINARY_INV);
		 */

		// Utils.matToBitmap(dstMat, rotated);

		// binaryImage.invalidate();

		// outlineFiltered();
	}

	public void outlineFiltered() {
		contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat(image.getWidth(), image.getHeight(),
				CvType.CV_8UC1, new Scalar(0));

		float ratio = (float) 3;
		int lowThreshold = (int) 50;
		int maxLowThreshold = 240;

		/*
		 * if (dstMat.type() == CvType.CV_8UC3 || dstMat.type() ==
		 * CvType.CV_8UC4) { Imgproc.cvtColor(dstMat, dstMat,
		 * Imgproc.COLOR_BGRA2GRAY); }
		 * 
		 * Imgproc.equalizeHist(dstMat, dstMat);
		 * 
		 * 
		 * 
		 * Imgproc.equalizeHist(contourMat, contourMat);
		 */

		Imgproc.Canny(dstMat, contourMat, lowThreshold * ratio, maxLowThreshold);

		Utils.matToBitmap(contourMat, image);
		// binaryImage.setImageBitmap(image);

		/*
		 * int dilation_size = 2; Mat element =
		 * Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2 *
		 * dilation_size + 1, 2 * dilation_size + 1), new Point(dilation_size,
		 * dilation_size));
		 * 
		 * Imgproc.dilate(contourMat, contourMat, element);
		 */

		Imgproc.findContours(contourMat, contours, hierarchy,
				Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		Point[] contourPoints;
		Point largestXPoint;
		Point smallestXPoint;
		Point largestYPoint;
		Point smallestYPoint;

		contourBitmap = image.copy(image.getConfig(), true);
		;

		Imgproc.drawContours(contourMat, contours, -1, new Scalar(255));

		Utils.matToBitmap(contourMat, contourBitmap);
		Utils.matToBitmap(dstMat, image);

		// contourImage.setImageBitmap(contourBitmap);
		// contourImage.invalidate();

		faceAnalyzer(contours, midpoint);

		for (int i = 0; i < contours.size(); i++) {
			contourPoints = contours.get(i).toArray();

			largestXPoint = contourPoints[0];
			smallestXPoint = contourPoints[0];
			largestYPoint = contourPoints[0];
			smallestYPoint = contourPoints[0];

			// find largest x in contour

			for (int j = 1; j < contourPoints.length; j++) {
				if (contourPoints[j].x > largestXPoint.x) {
					largestXPoint = contourPoints[j];
				}
			}
			// find smallest x in contour
			for (int j = 1; j < contourPoints.length; j++) {
				if (contourPoints[j].x < smallestXPoint.x) {
					smallestXPoint = contourPoints[j];
				}
			}

			// find largest y in contour
			for (int j = 1; j < contourPoints.length; j++) {
				if (contourPoints[j].y > largestYPoint.y) {
					largestYPoint = contourPoints[j];
				}
			}

			// find smallest y in contour
			for (int j = 1; j < contourPoints.length; j++) {
				if (contourPoints[j].y < smallestYPoint.y) {
					smallestYPoint = contourPoints[j];
				}
			}

			midPointContour = new Point(
					(largestXPoint.x + smallestXPoint.x) / 2,
					(largestYPoint.y + smallestYPoint.y) / 2);

		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		updateThreshold();

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	public boolean toon(Mat src, Mat dst) {
		Mat bgr = new Mat();
		Mat gray = new Mat();
		Mat edges = new Mat();
		Mat edgesBgr = new Mat();

		Imgproc.cvtColor(src, bgr, Imgproc.COLOR_BGRA2BGR);
		Imgproc.pyrMeanShiftFiltering(bgr.clone(), bgr, 3, 15);

		Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.Canny(gray, edges, 40, 80);

		Imgproc.cvtColor(edges, edgesBgr, Imgproc.COLOR_GRAY2BGR);

		Core.subtract(bgr, edgesBgr, bgr);

		Imgproc.cvtColor(bgr, dst, Imgproc.COLOR_BGR2BGRA);
		return true;

	}

	public void switchCameras(View v) {
		cameraView.switchCameras();
		frameLayoutCamera.bringChildToFront(cameraView);
		cameraInFront = true;
		imageViewInFront = false;
		zoomViewInFront = false;
	}

	public boolean wasFaceDetected() {
		if (numberOfFaceDetected > 0)
			return true;
		else
			return false;
	}

	public void faceAnalyzer(List<MatOfPoint> contours, PointF midpoint) {

		if (wasFaceDetected()) {
			leftEye = new Point(midpoint.x - myFace[0].eyesDistance() / 2,
					midpoint.y);
			rightEye = new Point(midpoint.x + myFace[0].eyesDistance() / 2,
					midpoint.y);
		}
	}

	public Point findHairline(List<MatOfPoint> contours) {
		double lowRatio = 0.7;
		double highRatio = 1;
		int constDeviation = 50;
		Point lowDistanceToHairline = new Point(midpoint.x - constDeviation,
				midpoint.y - myFace[0].eyesDistance() / lowRatio);
		Point highDistanceToHairline = new Point(midpoint.x + constDeviation,
				midpoint.y - myFace[0].eyesDistance() / highRatio);

		return findAveragePoint(contours, lowDistanceToHairline,
				highDistanceToHairline);
	}

	public Point[] findNose(List<MatOfPoint> contours) {
		double ratioOfEyeDistance = eyeDistance / 100;
		double lowRatio = 1.7;
		double highRatio = 1.1;
		int constDeviation = (int) (50 * ratioOfEyeDistance);
		Point lowDistanceToHairline = new Point(midpoint.x - constDeviation,
				midpoint.y + myFace[0].eyesDistance() / lowRatio);
		Point highDistanceToHairline = new Point(midpoint.x + constDeviation,
				midpoint.y + myFace[0].eyesDistance() / highRatio);

		return findLeftAndRightPoints(contours, lowDistanceToHairline,
				highDistanceToHairline);
	}

	public Point[] findMouth(List<MatOfPoint> contours) {
		double ratioOfEyeDistance = eyeDistance / 100;
		double lowRatio = 1 ;
		double highRatio = 0.8;
		int constDeviation = (int) (50 * ratioOfEyeDistance);
		Point lowDistanceToHairline = new Point(midpoint.x - constDeviation,
				midpoint.y + myFace[0].eyesDistance() / lowRatio);
		Point highDistanceToHairline = new Point(midpoint.x + constDeviation,
				midpoint.y + myFace[0].eyesDistance() / highRatio);

		return findLeftAndRightPoints(contours, lowDistanceToHairline,
				highDistanceToHairline);
	}

	public Point findChin(List<MatOfPoint> contours) {
		double lowRatio = 0.6;
		double highRatio = 0.5;
		int constDeviation = 50;
		Point lowDistanceToHairline = new Point(midpoint.x - constDeviation,
				midpoint.y + myFace[0].eyesDistance() / lowRatio);
		Point highDistanceToHairline = new Point(midpoint.x + constDeviation,
				midpoint.y + myFace[0].eyesDistance() / highRatio);

		return findAveragePoint(contours, lowDistanceToHairline,
				highDistanceToHairline);
	}

	public Point findLeftSide(List<MatOfPoint> contours) {
		double lowRatio = 0.85;
		double highRatio = 1.1;
		int constDeviation = 50;
		Point lowDistanceToHairline = new Point(midpoint.x
				- myFace[0].eyesDistance() / lowRatio, midpoint.y
				- constDeviation);
		Point highDistanceToHairline = new Point(midpoint.x
				- myFace[0].eyesDistance() / highRatio, midpoint.y
				+ constDeviation);

		return findAveragePoint(contours, lowDistanceToHairline,
				highDistanceToHairline);
	}

	public Point findRightSide(List<MatOfPoint> contours) {
		double lowRatio = 1.1;
		double highRatio = 0.85;
		int constDeviation = 50;
		Point lowDistanceToHairline = new Point(midpoint.x
				+ myFace[0].eyesDistance() / lowRatio, midpoint.y
				- constDeviation);
		Point highDistanceToHairline = new Point(midpoint.x
				+ myFace[0].eyesDistance() / highRatio, midpoint.y
				+ constDeviation);

		return findAveragePoint(contours, lowDistanceToHairline,
				highDistanceToHairline);
	}

	public Point findAveragePoint(List<MatOfPoint> contours,
			Point lowDistanceToHairline, Point highDistanceToHairline) {
		Point averagePoint = new Point(0, 0);
		int count = 0;

		for (int j = 0; j < contours.size(); j++) {
			Point[] contourPoints = contours.get(j).toArray();
			for (int i = 0; i < contourPoints.length; i++) {
				if (contourPoints[i].x > lowDistanceToHairline.x
						&& contourPoints[i].x < highDistanceToHairline.x
						&& contourPoints[i].y > lowDistanceToHairline.y
						&& contourPoints[i].y < highDistanceToHairline.y) {
					averagePoint.x += contourPoints[i].x;
					averagePoint.y += contourPoints[i].y;
					count++;
				}
			}
		}

		averagePoint.x /= count;
		averagePoint.y /= count;

		Canvas canvas = new Canvas(contourBitmap);
		canvas.drawRect((float) lowDistanceToHairline.x,
				(float) lowDistanceToHairline.y,
				(float) highDistanceToHairline.x,
				(float) highDistanceToHairline.y, paint2);

		return averagePoint;
	}

	public Point[] findLeftAndRightPoints(List<MatOfPoint> contours,
			Point lowDistanceToHairline, Point highDistanceToHairline) {
		Point leftPoint = new Point(frameLayoutCamera.getWidth(),
				frameLayoutCamera.getHeight());
		Point rightPoint = new Point(0, 0);
		double currentLeftPoint = leftPoint.x;
		double currentRightPoint = rightPoint.x;

		for (int j = 0; j < contours.size(); j++) {
			Point[] contourPoints = contours.get(j).toArray();
			for (int i = 0; i < contourPoints.length; i++) {
				if (contourPoints[i].x > lowDistanceToHairline.x
						&& contourPoints[i].x < highDistanceToHairline.x
						&& contourPoints[i].y > lowDistanceToHairline.y
						&& contourPoints[i].y < highDistanceToHairline.y) {
					if (contourPoints[i].x < currentLeftPoint
							&& Imgproc.contourArea(contours.get(j)) > 1
							&& contourPoints[i].x < midpoint.x) {
						currentLeftPoint = contourPoints[i].x;
						leftPoint = contourPoints[i];
					} else if (contourPoints[i].x > currentRightPoint
							&& Imgproc.contourArea(contours.get(j)) > 1
							&& contourPoints[i].x > midpoint.x) {
						currentRightPoint = contourPoints[i].x;
						rightPoint = contourPoints[i];
					}

				}
			}
		}

		Point[] leftAndRightPointsOnNose = new Point[2];
		leftAndRightPointsOnNose[0] = leftPoint;
		leftAndRightPointsOnNose[1] = rightPoint;

		Canvas canvas = new Canvas(contourBitmap);
		canvas.drawRect((float) lowDistanceToHairline.x,
				(float) lowDistanceToHairline.y,
				(float) highDistanceToHairline.x,
				(float) highDistanceToHairline.y, paint2);

		return leftAndRightPointsOnNose;
	}

	public void findFeatures(byte[] data) {

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

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
