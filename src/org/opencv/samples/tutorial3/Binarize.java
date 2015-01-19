package org.opencv.samples.tutorial3;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

public class Binarize {
	
	public static Bitmap createBinaryImage(Bitmap bm )
	{
	    int[] pixels = new int[bm.getWidth()*bm.getHeight()];
	    bm.getPixels( pixels, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight() );
	    int w = bm.getWidth();

	    // Calculate overall lightness of image
	    long gLightness = 0;
	    int lLightness;
	    int c;
	    for ( int x = 0; x < bm.getWidth(); x++ )
	    {
	        for ( int y = 0; y < bm.getHeight(); y++ )
	        {
	            c = pixels[x+y*w];
	            lLightness = ((c&0x00FF0000 )>>16) + ((c & 0x0000FF00 )>>8) + (c&0x000000FF);
	            pixels[x+y*w] = lLightness;
	            gLightness += lLightness;
	        }
	    }
	    gLightness /= bm.getWidth() * bm.getHeight();
	    gLightness = gLightness * 5 / 6;

	    // Extract features
	    boolean[][] binaryImage = new boolean[bm.getWidth()][bm.getHeight()];

	    for ( int x = 0; x < bm.getWidth(); x++ )
	        for ( int y = 0; y < bm.getHeight(); y++ )
	            binaryImage[x][y] = pixels[x+y*w] <= gLightness;

	    Bitmap blackAndWhite = null;
	    blackAndWhite.setPixels(pixels, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight());
	 
	    return blackAndWhite;
	}
	public static Bitmap toGrayscale(Bitmap bmpOriginal)
	{        
	    int width, height;
	    height = bmpOriginal.getHeight();
	    width = bmpOriginal.getWidth();    

	    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	    Canvas c = new Canvas(bmpGrayscale);
	    Paint paint = new Paint();
	    ColorMatrix cm = new ColorMatrix();
	    cm.setSaturation(0);
	    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
	    paint.setColorFilter(f);
	    c.drawBitmap(bmpOriginal, 0, 0, paint);
	    return bmpGrayscale;
	}

	

}
