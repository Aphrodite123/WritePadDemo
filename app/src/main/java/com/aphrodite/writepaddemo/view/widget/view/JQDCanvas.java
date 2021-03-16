package com.aphrodite.writepaddemo.view.widget.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.aphrodite.framework.utils.FileUtils;
import com.aphrodite.framework.utils.UIUtils;
import com.aphrodite.writepaddemo.model.Impl.JQDPainter;
import com.aphrodite.writepaddemo.model.api.IPathCallBack;
import com.aphrodite.writepaddemo.utils.BitmapUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private float mViewScale;

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

    //可撤销次数，默认为：5
    private int revokeTimes = 5;
    private List<Bitmap> mCacheBitmaps;

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
        mBitmap = BitmapUtils.drawBitmapBgColor(backgroundColor, mBgBitmap);
        if (null == mCanvas) {
            mCanvas = new Canvas(mBgBitmap);
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

    public void drawPath(UgeePoint ugeePoint) {
        if (null == mUgeePoint) {
            mUgeePoint = ugeePoint;
            return;
        }
        initCanvas();
        if (mUgeePoint.pressure <= 0 && ugeePoint.pressure > 0) {
            mPath.moveTo(mUgeePoint.x * mViewScale, mUgeePoint.y * mViewScale);
        }

        if (mUgeePoint.pressure > 0 && ugeePoint.pressure > 0) {
            if (null != mPaint) {
                mPaint.setStrokeWidth(lineWidth * calPressureScale(ugeePoint.pressure));
                mPaint.setColor(lineColor);
            }
            drawPath(new float[]{mUgeePoint.x * mViewScale, mUgeePoint.y * mViewScale, ugeePoint.x * mViewScale, ugeePoint.y * mViewScale}, mViewCanvas, mPaint);
            drawPath(new float[]{mUgeePoint.x * scale, mUgeePoint.y * scale, ugeePoint.x * scale, ugeePoint.y * scale}, mCanvas, mPaint);
            invalidate();
        }

        if (mUgeePoint.pressure > 0 && ugeePoint.pressure <= 0) {
            saveBitmap();
        }

        if (mUgeePoint.pressure <= 0 && ugeePoint.pressure <= 0) {
            if (null != mPath) {
                mPath.reset();
            }
        }
        mUgeePoint = ugeePoint;
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
            String result = BitmapUtils.saveBitmap(mBitmap, path, name, Bitmap.CompressFormat.JPEG, 100);
            if (!TextUtils.isEmpty(result) && null != callBack) {
                callBack.success(result);
            }
            return;
        } catch (IOException e) {
            e.printStackTrace();
            if (null != mViewBitmap) {
                mViewBitmap.recycle();
                mViewBitmap = null;
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
            mCacheBitmaps = new ArrayList<>();
        } else if (mCacheBitmaps.size() >= revokeTimes) {
            mCacheBitmaps.remove(0);
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
        mViewBitmap = mCacheBitmaps.get(mCacheBitmaps.size() - 1);
        invalidate();
    }

    /**
     * 撤销
     */
    public void revoke() {
        if (null == mCacheBitmaps || mCacheBitmaps.size() <= 0) {
            return;
        }
        mCacheBitmaps.remove(mCacheBitmaps.size() - 1);
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
        if (null != mViewCanvas) {
            mViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        if (null != mCanvas) {
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        invalidate();
    }

    public void destroy() {
        if (null != mViewBitmap) {
            mViewBitmap.recycle();
            mViewBitmap = null;
        }
        if (null != mViewCanvas) {
            mViewCanvas = null;
        }
        if (null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (null != mCanvas) {
            mCanvas = null;
        }
        if (null != mBgBitmap) {
            mBgBitmap.recycle();
            mBgBitmap = null;
        }
        if (null != mCacheBitmaps) {
            mCacheBitmaps.clear();
        }
    }

    public void setScale(float scale) {
        this.scale = scale;
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
