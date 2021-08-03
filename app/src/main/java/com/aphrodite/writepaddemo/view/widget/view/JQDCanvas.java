package com.aphrodite.writepaddemo.view.widget.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.aphrodite.writepaddemo.model.Impl.JQDPainter;
import com.aphrodite.writepaddemo.model.api.IPathCallBack;
import com.aphrodite.writepaddemo.utils.BitmapUtils;
import com.aphrodite.writepaddemo.utils.FileUtils;
import com.aphrodite.writepaddemo.utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.Nullable;
import cn.ugee.mi.optimize.UgeePoint;

/**
 * Created by Aphrodite on 2021/3/3.
 * 自定义画板View
 */
public class JQDCanvas extends View {
    private Context mContext;
    private int deviceWidth = 0x6B06;
    private int deviceHeight = 0x5014;
    private int maxPressure = 0x07FF;
    private int mWidth;
    private int mHeight;
    //缩放比，默认：0.03 注意该值不可过大，否则很容易出现OOM
    private float scale = (float) 0.03;
    private float mViewScale = (float) 0.03;

    private Paint mPaint;
    private Path mPath;
    private int backgroundColor;
    private float lineWidth;
    private int lineColor;

    //文件根目录
    private String mRootPath;
    private UgeePoint mUgeePoint;
    private Bitmap mViewBitmap;
    private Bitmap mBgBitmap;
    private Canvas mViewCanvas;
    private Bitmap mBitmap;
    private Canvas mCanvas;

    //可撤销次数，默认为：10
    private int revokeTimes = 10;
    private LinkedList<Bitmap> mCacheBitmaps;

    public JQDCanvas(Context context) {
        this(context, null);
    }

    public JQDCanvas(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JQDCanvas(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.backgroundColor = Color.WHITE;
        this.lineWidth = UIUtils.dip2px(context, 10);
        this.lineColor = Color.BLACK;

        initData();
    }

    public void init(String rootPath) {
        this.mRootPath = rootPath;
    }

    private void initData() {
        mPaint = new Paint();
        mPath = new Path();

        mPaint.setStrokeWidth(lineWidth);
        mPaint.setColor(lineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    private void initCanvas() {
        int width = (int) (mViewScale * deviceWidth);
        int height = (int) (mViewScale * deviceHeight);
        if (null == mViewBitmap) {
            mViewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        if (null == mViewCanvas) {
            mViewCanvas = new Canvas(mViewBitmap);
        }
        if (null == mBgBitmap) {
            mBgBitmap = Bitmap.createBitmap((int) (scale * deviceWidth), (int) (scale * deviceHeight), Bitmap.Config.ARGB_8888);
        }
        if (null == mBitmap) {
            mBitmap = BitmapUtils.drawBitmapBgColor(backgroundColor, mBgBitmap);
        }
        if (null == mCanvas) {
            mCanvas = new Canvas(mBitmap);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        mViewScale = calScale();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null != mViewBitmap) {
            canvas.drawBitmap(mViewBitmap, 0, 0, null);
        }
    }

    public void displayPoints(List<UgeePoint> ugeePoints) {
        if (null == ugeePoints || ugeePoints.size() <= 0) {
            return;
        }
        initCanvas();
        if (null != mUgeePoint) {
            ugeePoints.add(0, mUgeePoint);
        }
        for (int i = 0; i < ugeePoints.size() - 1; i++) {
            drawPath(ugeePoints.get(i), ugeePoints.get(i + 1));
        }
        mUgeePoint = ugeePoints.get(ugeePoints.size() - 1);
        invalidate();
    }

    private void drawPath(UgeePoint ugeePoint, UgeePoint nextUgeePoint) {
        if (null == ugeePoint || null == nextUgeePoint) {
            return;
        }
        if (ugeePoint.state <= 0 && nextUgeePoint.state > 0) {
            mPath.moveTo(ugeePoint.x * mViewScale, ugeePoint.y * mViewScale);
        }
        if (ugeePoint.state > 0 && nextUgeePoint.state > 0) {
            if (null != mPaint) {
                float width = lineWidth * calPressureScale(ugeePoint.pressure);
                mPaint.setStrokeWidth(width);
                mPaint.setColor(lineColor);
            }
            drawPath(new float[]{ugeePoint.x * mViewScale, ugeePoint.y * mViewScale, nextUgeePoint.x * mViewScale, nextUgeePoint.y * mViewScale}, mViewCanvas, mPaint);
            drawPath(new float[]{ugeePoint.x * scale, ugeePoint.y * scale, nextUgeePoint.x * scale, nextUgeePoint.y * scale}, mCanvas, mPaint);
        }
        if (ugeePoint.state <= 0 && nextUgeePoint.state <= 0) {
            if (null != mPath) {
                mPath.reset();
            }
        }
        if (ugeePoint.state > 0 && nextUgeePoint.state <= 0) {
            saveBitmap();
        }
    }

    private void drawPath(float[] points, Canvas canvas, Paint paint) {
        if (null == points || points.length < 4) {
            return;
        }
        float x = points[2] - points[0];
        float y = points[3] - points[1];
        //10倍插点
        int insertCount = (int) (Math.max(Math.abs(x), Math.abs(y)) + 2);
        //S.i("补点：$insertCount")
        float dx = x / insertCount;
        float dy = y / insertCount;
        for (int i = 0; i < insertCount; i++) {
            float insertX = points[0] + i * dx;
            float insertY = points[1] + i * dy;
            if (null != canvas && null != paint) {
                canvas.drawPoint(insertX, insertY, paint);
            }
        }
    }

    /**
     * 保存图片
     */
    public void createImageWithFilename(String fileName, IPathCallBack callBack) {
        String realPath = mRootPath + fileName;
        if (TextUtils.isEmpty(realPath)) {
            if (null != callBack) {
                callBack.failed(JQDPainter.Error.ERROR_FIVE);
            }
            return;
        }
        if (FileUtils.isFileExist(realPath)) {
            if (null != callBack) {
                callBack.failed(JQDPainter.Error.ERROR_TWO);
            }
            return;
        }
        String path = realPath.substring(0, realPath.lastIndexOf(File.separator) + 1);
        String name = realPath.substring(realPath.lastIndexOf(File.separator) + 1);
        if (!FileUtils.makeDirs(path)) {
            if (null != callBack) {
                callBack.failed(JQDPainter.Error.ERROR_THREE);
            }
            return;
        }
        try {
            String result = BitmapUtils.saveBitmap(mBitmap, path, name, Bitmap.CompressFormat.PNG, 100);
            if (!TextUtils.isEmpty(result) && null != callBack) {
                callBack.success(result);
            }
            return;
        } catch (IOException e) {
            e.printStackTrace();
            if (null != mBitmap) {
                mBitmap.recycle();
                mBitmap = null;
            }
            if (null != callBack) {
                callBack.failed(JQDPainter.Error.ERROR_FOUR);
            }
        }
    }

    /**
     * 缓存轨迹
     */
    private void saveBitmap() {
        if (null == mViewBitmap) {
            return;
        }
        Bitmap copyBitmap = mViewBitmap.copy(Bitmap.Config.ARGB_8888, true);
        if (null == mCacheBitmaps) {
            mCacheBitmaps = new LinkedList<>();
        } else if (mCacheBitmaps.size() > revokeTimes) {
            mCacheBitmaps.removeFirst();
        }
        mCacheBitmaps.add(copyBitmap);
    }

    /**
     * 重绘
     */
    public void reDraw() {
        if (null == mCacheBitmaps || mCacheBitmaps.size() <= 0) {
            return;
        }
        Bitmap copyBitmap = mCacheBitmaps.getLast().copy(Bitmap.Config.ARGB_8888, true);
        mViewBitmap = copyBitmap;
        mViewCanvas.setBitmap(mViewBitmap);
        invalidate();
    }

    /**
     * 撤销
     */
    public void revoke() {
        if (!canRevoke()) {
            return;
        }
        mCacheBitmaps.removeLast();
        reDraw();
    }

    /**
     * 是否可以撤销上一步
     *
     * @return
     */
    public boolean canRevoke() {
        if (null == mCacheBitmaps || mCacheBitmaps.size() <= 0) {
            return false;
        }
        return true;
    }

    public void clear() {
        mViewCanvas = null;
        mCanvas = null;
        if (null != mViewBitmap) {
            mViewBitmap.recycle();
            mViewBitmap = null;
        }
        if (null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (null != mBgBitmap) {
            mBgBitmap.recycle();
            mBgBitmap = null;
        }
        if (null != mCacheBitmaps) {
            mCacheBitmaps.clear();
        }
        mUgeePoint = null;
        invalidate();
    }

    public void destroy() {
        mViewCanvas = null;
        mCanvas = null;
        if (null != mViewBitmap) {
            mViewBitmap.recycle();
            mViewBitmap = null;
        }
        if (null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (null != mBgBitmap) {
            mBgBitmap.recycle();
            mBgBitmap = null;
        }
        if (null != mCacheBitmaps) {
            mCacheBitmaps.clear();
        }
        mUgeePoint = null;
    }

    public void setScale(Context context, float scale) {
        this.scale = scale * UIUtils.getDensity(context);
    }

    public void setDeviceWidth(int width) {
        this.deviceWidth = width;
    }

    public void setDeviceHeight(int height) {
        this.deviceHeight = height;
    }

    public void setDevicePressure(int p) {
        this.maxPressure = p;
    }

    public void setLineWidth(float width) {
        this.lineWidth = width;
        if (null != mPaint) {
            mPaint.setStrokeWidth(width);
        }
    }

    public void setLineColor(int color) {
        this.lineColor = color;
        if (null != mPaint) {
            mPaint.setColor(color);
        }
    }

    public void setImageBgColor(int color) {
        this.backgroundColor = color;
    }

    public void setRevokeTimes(int times) {
        this.revokeTimes = times;
    }

    private float calScale() {
        float xScale = (float) mWidth / deviceWidth;
        float yScale = (float) mHeight / deviceHeight;
        return Math.min(xScale, yScale);
    }

    private float calPressureScale(float p) {
        return p / maxPressure;
    }

}
