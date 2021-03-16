package com.aphrodite.writepaddemo.model.Impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextUtils;

import com.aphrodite.framework.utils.FileUtils;
import com.aphrodite.framework.utils.UIUtils;
import com.aphrodite.writepaddemo.config.AppConfig;
import com.aphrodite.writepaddemo.model.api.IBasePathDerive;
import com.aphrodite.writepaddemo.model.api.IPathCallBack;
import com.aphrodite.writepaddemo.model.ffmpeg.FFmpegTask;
import com.aphrodite.writepaddemo.model.ffmpeg.ffmpegListener;
import com.aphrodite.writepaddemo.utils.BitmapUtils;
import com.aphrodite.writepaddemo.utils.FFmpegUtils;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.ugee.mi.optimize.UgeePoint;

/**
 * Created by Aphrodite on 2021/3/3.
 * 智能手写板轨迹数据生成图片，视频等接口实现
 */
public class JQDPainter implements IBasePathDerive {
    private static final String TAG = JQDPainter.class.getSimpleName();
    private Context mContext;
    private static JQDPainter mInstance = null;
    private boolean mIsGetImage = true;

    //文件根目录
    private String mRootPath;
    private String mTempImagePath;
    private List<UgeePoint> mUgeePoints;
    private IPathCallBack mCallBack;

    private int deviceWidth = 0x6B06;
    private int deviceHeight = 0x5014;
    private int maxPressure = 0x07FF;
    //缩放比，默认：0.03 注意该值不可过大，否则很容易出现OOM
    private float scale = (float) 0.03;

    private Bitmap mBitmap;
    private Bitmap mBgBitmap;
    private Canvas mCanvas;
    private int pointsPerFrame;

    private Paint mPaint;
    private Path mPath;
    private int backgroundColor;
    private int lineWidth;
    private int lineColor;

    public static JQDPainter getInstance(Context context) {
        if (null == mInstance) {
            synchronized (JQDPainter.class) {
                if (null == mInstance) {
                    mInstance = new JQDPainter(context);
                }
            }
        }
        return mInstance;
    }

    private JQDPainter(Context context) {
        this.mContext = context;
        this.mUgeePoints = new ArrayList<>();
        this.backgroundColor = Color.WHITE;
        this.lineWidth = UIUtils.dip2px(context, 10);
        this.lineColor = Color.BLACK;
        initData();
    }

    public void init(String rootPath) {
        this.mRootPath = rootPath;
        this.mTempImagePath = rootPath + "temp/";
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
        int width = (int) (scale * deviceWidth);
        int height = (int) (scale * deviceHeight);
        mBgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mBitmap = BitmapUtils.drawBitmapBgColor(backgroundColor, mBgBitmap);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    public void createImageWithPoints(String filename, IPathCallBack callBack) {
        this.mIsGetImage = true;
        this.mCallBack = callBack;
        String realPath = mRootPath + filename;
        if (TextUtils.isEmpty(realPath)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_FIVE);
            }
            return;
        }
        if (FileUtils.isFileExist(realPath)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_TWO);
            }
            return;
        }
        String path = realPath.substring(0, realPath.lastIndexOf(File.separator) + 1);
        String name = realPath.substring(realPath.lastIndexOf(File.separator) + 1);
        if (!FileUtils.makeDirs(path)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_THREE);
            }
            return;
        }
        initCanvas();
        drawPath(mUgeePoints);
        saveImage(mBitmap, path, name, Bitmap.CompressFormat.JPEG, 100);
    }

    @Override
    public void createVideoWithPoints(String filename, int revokeTimes, IPathCallBack callBack) {
        this.mIsGetImage = false;
        this.pointsPerFrame = revokeTimes;
        this.mCallBack = callBack;
        String realPath = mRootPath + filename;
        if (TextUtils.isEmpty(realPath)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_FIVE);
            }
            return;
        }
        if (FileUtils.isFileExist(realPath)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_TWO);
            }
            return;
        }
        String path = realPath.substring(0, realPath.lastIndexOf(File.separator) + 1);
        if (!FileUtils.makeDirs(path)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_THREE);
            }
            return;
        }
        initCanvas();
        drawPath(mUgeePoints);
        imageToVideo(mTempImagePath, realPath);
    }

    @Override
    public void createPDFWithPoints(String filename, IPathCallBack callBack) {
        this.mCallBack = callBack;
        String realPath = mRootPath + filename;
        if (TextUtils.isEmpty(realPath)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_FIVE);
            }
            return;
        }
        if (FileUtils.isFileExist(realPath)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_TWO);
            }
            return;
        }
        String path = realPath.substring(0, realPath.lastIndexOf(File.separator) + 1);
        if (!FileUtils.makeDirs(path)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_THREE);
            }
            return;
        }
        initCanvas();
        drawPath(mUgeePoints);
        createPdf(realPath);
    }

    private void drawPath(List<UgeePoint> ugeePoints) {
        if (null == ugeePoints || ugeePoints.size() <= 0) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_ONE);
            }
            return;
        }
        UgeePoint ugeePoint = null;
        UgeePoint nextUgeePoint = null;
        int num = 0;
        int index = 1;
        StringBuilder pictureAbsPath = null;
        for (int i = 0; i < ugeePoints.size() - 1; i++) {
            ugeePoint = ugeePoints.get(i);
            nextUgeePoint = ugeePoints.get(i + 1);
            if (null == ugeePoint || null == nextUgeePoint) {
                continue;
            }

            if (ugeePoint.pressure <= 0 && nextUgeePoint.pressure > 0) {
                if (null != mPath) {
                    mPath.moveTo(ugeePoint.x * scale, ugeePoint.y * scale);
                }
            }

            if (ugeePoint.pressure > 0 && nextUgeePoint.pressure > 0) {
                if (null != mPaint) {
                    float width = lineWidth * calPressureScale(ugeePoint.pressure);
                    mPaint.setStrokeWidth(width);
                    mPaint.setColor(lineColor);
                }
                if (null != mPath) {
                    mPath.quadTo(ugeePoint.x * scale, ugeePoint.y * scale, nextUgeePoint.x * scale, nextUgeePoint.y * scale);
                }
                drawPath(new float[]{ugeePoint.x * scale, ugeePoint.y * scale, nextUgeePoint.x * scale, nextUgeePoint.y * scale}, mCanvas, mPaint);
                if (!mIsGetImage) {
                    num++;
                    if (num >= pointsPerFrame) {
                        pictureAbsPath = new StringBuilder();
                        pictureAbsPath.append(index).append(AppConfig.IMAGE_SUFFIX);
                        saveImage(mBitmap, mTempImagePath, pictureAbsPath.toString(), Bitmap.CompressFormat.JPEG, 100);
                        num = 0;
                        index++;
                    }
                }
            }

            if (ugeePoint.pressure <= 0 && nextUgeePoint.pressure <= 0) {
                if (null != mPath) {
                    mPath.reset();
                }
            }
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

    private void saveImage(Bitmap bitmap, String path, String fileName, Bitmap.CompressFormat format, int quality) {
        try {
            String result = BitmapUtils.saveBitmap(bitmap, path, fileName, format, quality);
            if (!TextUtils.isEmpty(result) && null != mCallBack) {
                mCallBack.success(result);
            }
            return;
        } catch (IOException e) {
            e.printStackTrace();
            if (null != bitmap) {
                bitmap.recycle();
                bitmap = null;
            }
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_FOUR);
            }
        }
    }

    /**
     * 图片转视频
     *
     * @param path
     * @param fileName
     */
    private void imageToVideo(String srcPath, String fileName) {
        int frameRate = 10;// 合成视频帧率建议:1-10  普通视频帧率一般为25
        String[] commandLine = FFmpegUtils.pictureToVideo(srcPath, frameRate, fileName);
        FFmpegTask.execute(commandLine, new ffmpegListener() {
            @Override
            public void onBegin() {

            }

            @Override
            public void onEnd(int resultCode, String resultMsg) {
                if (null != mCallBack) {
                    mCallBack.success(fileName);
                }
            }
        });
    }

    private void createPdf(String filename) {
        PdfImpl pdf = new PdfImpl.Builder(filename, PageSize.A4).build();
        try {
            pdf.init().addImageToPdf(mBitmap, Element.ALIGN_CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            pdf.close();
        }
    }

    public void addPoints(List<UgeePoint> ugeePoints) {
        if (null != mUgeePoints && mUgeePoints.size() > 0) {
            mUgeePoints.clear();
        }
        mUgeePoints.addAll(ugeePoints);
    }

    public void setDeviceWidth(int width) {
        this.deviceWidth = width;
    }

    public void setDeviceHeight(int height) {
        this.deviceHeight = height;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setDevicePressure(int p) {
        this.maxPressure = p;
    }

    public void setImageBgColor(int color) {
        this.backgroundColor = color;
    }

    public void setPathWidth(int width) {
        this.lineWidth = width;
        if (null != mPaint) {
            mPaint.setStrokeWidth(width);
        }
    }

    public void setPathColor(int color) {
        this.lineColor = color;
        if (null != mPaint) {
            mPaint.setColor(color);
        }
    }

    public void setPathStyle(Paint.Style style) {
        if (null != mPaint) {
            mPaint.setStyle(style);
        }
    }

    public void setPathAntiAlias(boolean antiAlias) {
        if (null != mPaint) {
            mPaint.setAntiAlias(antiAlias);
        }
    }

    public void setPathStrokeCap(Paint.Cap cap) {
        if (null != mPaint) {
            mPaint.setStrokeCap(cap);
        }
    }

    public void destroy() {
        if (null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }

        if (null != mBgBitmap) {
            mBgBitmap.recycle();
            mBgBitmap = null;
        }

        if (null != mCanvas) {
            mCanvas = null;
        }
    }

    private float calPressureScale(float p) {
        return p / maxPressure;
    }

    public interface Error {
        int BASE = 10000;
        //轨迹为空
        int ERROR_ONE = BASE + 1;
        //文件已存在
        int ERROR_TWO = BASE + 2;
        //文件创建失败
        int ERROR_THREE = BASE + 3;
        //文件保存失败
        int ERROR_FOUR = BASE + 4;
        //文件路径名称为空
        int ERROR_FIVE = BASE + 5;
    }

}
