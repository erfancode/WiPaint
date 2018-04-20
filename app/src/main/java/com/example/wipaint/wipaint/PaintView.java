package com.example.wipaint.wipaint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Erfan on 21/03/2018.
 */

public class PaintView extends View
{
    public static int BRUSH_SIZE = 20;
    public static int DEFAULT_COLOR = Color.RED;
    public static final int DEFAULT_BG_COLOR = Color.WHITE;
    private static final float TOUCH_TOLERANCE = 4;
    public boolean blur, emboss, eraser = false;
    private MaskFilter mBlur, mEmboss;
    private float mX, mY;
    private double paintLatitude = 0, paintLongitude = 0;
    public Paint mPaint;
    private Path mPath;
    private ArrayList<FingerPath> paths = new ArrayList<>();
    private int currentColor = Color.RED;
    private int backgroundColor = DEFAULT_BG_COLOR;
    private int strokeWidth;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);


    private ConnectionsActivity connectionsActivity;
    public PaintView(Context context)
    {
        this(context, null);
        Log.e("PaintView", "const1");
    }

    public PaintView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        Log.e("PaintView", "const2");
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);

        mEmboss = new EmbossMaskFilter(new float[]{1, 1, 1}, 0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);
    }

    public void init(DisplayMetrics metrics)
    {
        Log.e("PaintView", "init");
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        currentColor = DEFAULT_COLOR;
        strokeWidth = BRUSH_SIZE;
    }

    public void clear()
    {
        Log.e("PaintView", "clear");
        backgroundColor = DEFAULT_BG_COLOR;
        paths.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        Log.e("PaintView", "onDraw");
        canvas.save();
        mCanvas.drawColor(backgroundColor);
        for (FingerPath fp : paths)
        {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mPaint.setMaskFilter(null);
            if(fp.emboss)
            {
                mPaint.setMaskFilter(mEmboss);
            }
            else if (fp.blur)
            {
                mPaint.setMaskFilter(mBlur);
            }

            mCanvas.drawPath(fp.path, mPaint);
        }
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
    }

    private void touchStart(float x, float y)
    {
        Log.e("PaintView", "start");
        mPath = new Path();
        FingerPath fp = new FingerPath(currentColor, strokeWidth, mPath, blur, emboss);
        paths.add(fp);
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y)
    {
        Log.e("PaintView", "move");
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
        {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp()
    {
        Log.e("PaintView", "up");
        mPath.lineTo(mX, mY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        Log.e("PaintView", "Ev");
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }
        return true;
    }

    public ArrayList<FingerPath> getPaths()
    {
        return paths;
    }
    public void addPaths(ArrayList<FingerPath> e)
    {
        paths.addAll(e);
    }
    public void setPaths(ArrayList<FingerPath> f)
    {
        paths = f;
    }
    public void setCurrentColor(int i)
    {
        currentColor = i;
    }
    public int getCurrentColor()
    {
        return currentColor;
    }
    public void setStrokeWidth(int i)
    {
        strokeWidth = i;
    }
    public int getStrokeWidth()
    {
        return strokeWidth;
    }
    public void normal()
    {
        blur = false;
        emboss = false;
    }
    public void emboss()
    {
        emboss = true;
        blur = false;
    }
    public void blur()
    {
        emboss = false;
        blur = true;
    }
    public void savePaint()
    {

        Thread savingFileThread = new Thread()
        {
            @Override
            public void run()
            {
                ExifInterface exif = null;
                DateFormat dateFormate = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");
                String name = dateFormate.format(Calendar.getInstance().getTime());

                String path = Environment.getExternalStorageDirectory().toString() + "/wipaint/";
                OutputStream fileOut = null;
                File dir = new File(path);
                dir.mkdirs();
                File file = new File(dir, name + ".JPEG");
                Log.v("PaintView", "save on : " + path);
                //Toast.makeText(getContext(), R.string.save_msg + "\n" + path, Toast.LENGTH_LONG).show();
                try
                {
                    fileOut = new FileOutputStream(file);
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
                    fileOut.flush();
                    fileOut.close();
                    exif = new ExifInterface(path+name+ ".JPEG");
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, String.valueOf(paintLatitude));
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, String.valueOf(paintLongitude));
                    exif.saveAttributes();
                    Log.v("PaintView", "savePaint GPS : " + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) + "   " + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                } catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        };
        savingFileThread.start();
    }
    public void setLatLon(double lat, double lon)
    {
        paintLatitude = lat;
        paintLongitude = lon;
    }
    public ArrayList<Double> getLatLong()
    {
        ArrayList<Double> ret = new ArrayList<Double>();
        ret.add(paintLatitude);
        ret.add(paintLongitude);
        return ret;
    }

}
