package com.aphrodite.writepaddemo.model.Impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextUtils;
import android.util.Log;

import com.aphrodite.writepaddemo.model.api.IBasePathDerive;
import com.aphrodite.writepaddemo.model.api.IPathCallBack;
import com.aphrodite.writepaddemo.model.provider.BitmapProvider;
import com.aphrodite.writepaddemo.utils.BitmapUtils;
import com.aphrodite.writepaddemo.utils.FileUtils;
import com.aphrodite.writepaddemo.utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import cn.ugee.mi.optimize.OnPenCallBack;
import cn.ugee.mi.optimize.UgeePenOptimizeClass;
import cn.ugee.mi.optimize.UgeePoint;
import xyz.mylib.creator.handler.CreatorExecuteResponseHander;
import xyz.mylib.creator.task.AvcExecuteAsyncTask;

/**
 * Created by Aphrodite on 2021/3/3.
 * 智能手写板轨迹数据生成图片，视频等接口实现
 */
public class JQDPainter implements IBasePathDerive {
    private static final String TAG = JQDPainter.class.getSimpleName();
    private Context mContext;
    private boolean mIsGetImage = true;

    private float mBackgroundColor = Color.WHITE;
    private float mLineColor = Color.BLACK;
    private float mLineWidth;
    //缩放比，默认：0.03 注意该值不可过大，否则很容易出现OOM
    private float mScale = (float) 0.01;
    private float maxPressure = 0x07FF;
    private float mPointsPerFrame = 20;
    private float mFps = 10;
    private float mWidth = 0x5078;
    private float mHeight = 0x6B08;

    private ExecutorService mExecutors;
    private FutureTask mFutureTask;

    //文件根目录
    private String mRootPath;
    private List<UgeePoint> mUgeePoints;
    private IPathCallBack mCallBack;

    private Bitmap mBitmap;
    private Bitmap mBgBitmap;
    private Canvas mCanvas;
    private Paint mPaint;
    private Path mPath;
    private BitmapProvider mBitmapProvider;

    public JQDPainter(Context context) {
        this.mContext = context;
        this.mLineWidth = UIUtils.dip2px(context, 10);
        initData();
    }

    public void init(String rootPath) {
        this.mRootPath = rootPath;
    }

    private void initData() {
        mPaint = new Paint();
        mPath = new Path();

        mPaint.setStrokeWidth(mLineWidth);
        mPaint.setColor((int) mLineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mBitmapProvider = new BitmapProvider();
    }

    private void initCanvas() {
        int width = (int) (mScale * mWidth);
        int height = (int) (mScale * mHeight);
        mBgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        mBitmap = BitmapUtils.drawBitmapBgColor((int) mBackgroundColor, mBgBitmap);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    public void createMediaWithPoints(List<UgeePoint> ugeePoints, String type, String filename, Map<String, Object> params, IPathCallBack callBack) {
        setConfig(params);
        mExecutors = Executors.newSingleThreadExecutor();
        mFutureTask = new FutureTask(new CreateMediaCallable(ugeePoints, type, filename, callBack));
        mExecutors.execute(mFutureTask);
    }

    @Override
    public void createPDFWithText(String text, String filename, Map<String, Object> params, IPathCallBack callBack) {
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
        textToPdf(text, realPath, params);
    }

    private void optimizePoints(List<UgeePoint> ugeePoints, String type, String fileName, IPathCallBack callBack) {
        if (null == ugeePoints || ugeePoints.size() <= 0) {
            return;
        }
        mUgeePoints = new ArrayList<>();
        UgeePenOptimizeClass ugeePenOptimizeClass = new UgeePenOptimizeClass(new OnPenCallBack() {
            @Override
            public void onPenOptimizeDate(UgeePoint ugeePoint) {
                mUgeePoints.add(ugeePoint);
            }

            @Override
            public void onCompleteDate(boolean b) {
                if (!b) {
                    return;
                }
                //轨迹点集优化完成
                switch (type) {
                    case Type.IMAGE:
                        createImageWithPoints(fileName, callBack);
                        break;
                    case Type.VIDEO:
                        createVideoWithPoints(fileName, callBack);
                        break;
                    case Type.PDF:
                        createPDFWithPoints(fileName, callBack);
                        break;
                }
            }
        });
        ugeePenOptimizeClass.customListPoint(ugeePoints);
    }

    private void setConfig(Map<String, Object> params) {
        if (null == params || params.size() <= 0) {
            return;
        }
        mBackgroundColor = params.containsKey(ParamsKey.BACKGROUND_COLOR) ?
                Color.parseColor((String) params.get(ParamsKey.BACKGROUND_COLOR)) : Color.WHITE;
        mLineColor = params.containsKey(ParamsKey.LINE_COLOR) ?
                Color.parseColor((String) params.get(ParamsKey.LINE_COLOR)) : Color.BLACK;
        mLineWidth = params.containsKey(ParamsKey.LINE_WIDTH) ?
                new Double((Double) params.get(ParamsKey.LINE_WIDTH)).floatValue() : (float) 1.5;
        mLineWidth = mLineWidth * UIUtils.getDensity(mContext);
        mScale = params.containsKey(ParamsKey.SCALE) ?
                new Double((Double) params.get(ParamsKey.SCALE)).floatValue() : (float) 0.01;
        mScale = mScale * UIUtils.getDensity(mContext);
        maxPressure = params.containsKey(ParamsKey.MAX_PRESSURE) ?
                new Double((Double) params.get(ParamsKey.MAX_PRESSURE)).floatValue() : 0x07FF;
        mPointsPerFrame = params.containsKey(ParamsKey.POINTS_PERFRAME) ?
                new Double((Double) params.get(ParamsKey.POINTS_PERFRAME)).floatValue() : 20;
        mFps = params.containsKey(ParamsKey.FPS) ?
                new Double((Double) params.get(ParamsKey.FPS)).floatValue() : 10;
        Map<String, Object> deviceSize = params.containsKey(ParamsKey.SIZE) ?
                (Map<String, Object>) params.get(ParamsKey.SIZE) : null;
        if (null != deviceSize && deviceSize.size() > 0) {
            mWidth = deviceSize.containsKey(ParamsKey.WIDTH) ?
                    new Double((Double) deviceSize.get(ParamsKey.WIDTH)).floatValue() : 0x5078;
            mHeight = deviceSize.containsKey(ParamsKey.HEIGHT) ?
                    new Double((Double) deviceSize.get(ParamsKey.HEIGHT)).floatValue() : 0x6B08;
        }
    }

    private void createImageWithPoints(String filename, IPathCallBack callBack) {
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
        saveImage(mBitmap, path, name, Bitmap.CompressFormat.PNG, 100);
    }

    private void createVideoWithPoints(String filename, IPathCallBack callBack) {
        this.mIsGetImage = false;
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
        imageToVideo(realPath);
        drawPath(mUgeePoints);
    }

    private void createPDFWithPoints(String filename, IPathCallBack callBack) {
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
        if (!FileUtils.makeDirs(path)) {
            if (null != mCallBack) {
                mCallBack.failed(Error.ERROR_THREE);
            }
            return;
        }
        initCanvas();
        drawPath(mUgeePoints);
        imageToPdf(mBitmap, realPath);
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
        for (int i = 0; i < ugeePoints.size() - 1; i++) {
            ugeePoint = ugeePoints.get(i);
            nextUgeePoint = ugeePoints.get(i + 1);
            if (null == ugeePoint || null == nextUgeePoint) {
                continue;
            }

            if (ugeePoint.state <= 0 && nextUgeePoint.state > 0) {
                if (null != mPath) {
                    mPath.moveTo(ugeePoint.x * mScale, ugeePoint.y * mScale);
                }
            }

            if (ugeePoint.state > 0 && nextUgeePoint.state > 0) {
                if (null != mPaint) {
                    float width = mLineWidth * calPressureScale(ugeePoint.pressure);
                    mPaint.setStrokeWidth(width);
                    mPaint.setColor((int) mLineColor);
                }
                if (null != mPath) {
                    mPath.quadTo(ugeePoint.x * mScale, ugeePoint.y * mScale, nextUgeePoint.x * mScale, nextUgeePoint.y * mScale);
                }
                drawPath(new float[]{ugeePoint.x * mScale, ugeePoint.y * mScale, nextUgeePoint.x * mScale, nextUgeePoint.y * mScale}, mCanvas, mPaint);
                if (!mIsGetImage) {
                    num++;
                    if (num >= mPointsPerFrame) {
                        Bitmap bitmap = mBitmap.copy(Bitmap.Config.ARGB_4444, true);
                        mBitmapProvider.setQueue(bitmap);
                        num = 0;
                    }
                }
            }

            if (ugeePoint.state <= 0 && nextUgeePoint.state <= 0) {
                if (null != mPath) {
                    mPath.reset();
                }
            }
        }

        //校验是否添加最后一帧
        if (!mIsGetImage && num > 0) {
            Bitmap bitmap = mBitmap.copy(Bitmap.Config.ARGB_4444, true);
            mBitmapProvider.setQueue(bitmap);
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
    private void imageToVideo(String fileName) {
        AvcExecuteAsyncTask.execute(mBitmapProvider, (int) mFps,
                new CreatorExecuteResponseHander() {
                    @Override
                    public void onSuccess(Object message) {
                        if (null != mBitmapProvider) {
                            mBitmapProvider.finish();
                        }
                        if (null != mCallBack) {
                            mCallBack.success(fileName);
                        }
                        Log.d(TAG, "Enter to onSuccess.");
                    }

                    @Override
                    public void onProgress(Object message) {
                        Log.d(TAG, "Enter to onProgress." + message);
                    }

                    @Override
                    public void onFailure(Object message) {
                        if (null != mBitmapProvider) {
                            mBitmapProvider.finish();
                        }
                        if (null != mCallBack) {
                            mCallBack.failed(Error.ERROR_THREE);
                        }
                    }

                    @Override
                    public void onStart() {
                        Log.d(TAG, "Enter to onStart.");
                    }

                    @Override
                    public void onFinish() {
                        if (null != mBitmapProvider) {
                            mBitmapProvider.finish();
                        }
                        Log.d(TAG, "Enter to onFinish.");
                    }
                }, fileName);
    }

    private void imageToPdf(Bitmap bitmap, String filename) {
        if (null == bitmap || TextUtils.isEmpty(filename)) {
            return;
        }

        Map<String, Object> pageSize = new HashMap<>();
        pageSize.put(PdfImpl.ParamsKey.WIDTH, Double.valueOf(bitmap.getWidth()));
        pageSize.put(PdfImpl.ParamsKey.HEIGHT, Double.valueOf(bitmap.getHeight()));
        Map<String, Object> params = new HashMap<>();
        params.put(PdfImpl.ParamsKey.PAGE_SIZE, pageSize);
        PdfImpl.Builder builder = new PdfImpl.Builder(mContext, filename, params);
        PdfImpl pdf = builder.build();
        boolean result = pdf.init().addImageToPdf(bitmap).save();
        if (null == mCallBack) {
            return;
        }
        if (result) {
            mCallBack.success(filename);
        } else {
            mCallBack.failed(Error.ERROR_FOUR);
        }
    }

    private void textToPdf(String text, String filename, Map<String, Object> params) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(filename)) {
            return;
        }
        PdfImpl.Builder builder = new PdfImpl.Builder(mContext, filename, params);
        PdfImpl pdf = builder.build();
        boolean result = pdf.init().addTextToPdf(text).save();
        if (null == mCallBack) {
            return;
        }
        if (result) {
            mCallBack.success(filename);
        } else {
            mCallBack.failed(Error.ERROR_FOUR);
        }
    }

    public void addPoints(List<UgeePoint> ugeePoints) {
        if (null != mUgeePoints && mUgeePoints.size() > 0) {
            mUgeePoints.clear();
        }
        mUgeePoints.addAll(ugeePoints);
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

    public interface ParamsKey {
        String BACKGROUND_COLOR = "backgroundColor";
        String LINE_COLOR = "lineColor";
        String LINE_WIDTH = "lineWidth";
        String SCALE = "scale";
        String SIZE = "size";
        String MAX_PRESSURE = "maxPressure";
        String POINTS_PERFRAME = "pointsPerFrame";
        String FPS = "fps";
        String WIDTH = "width";
        String HEIGHT = "height";
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

    private class CreateMediaCallable implements Callable<String> {
        private List<UgeePoint> ugeePoints;
        private String type;
        private String fileName;
        private IPathCallBack callBack;

        public CreateMediaCallable(List<UgeePoint> ugeePoints, String type, String fileName, IPathCallBack callBack) {
            this.ugeePoints = ugeePoints;
            this.type = type;
            this.fileName = fileName;
            this.callBack = callBack;
        }

        @Override
        public String call() throws Exception {
            optimizePoints(ugeePoints, type, fileName, callBack);
            return null;
        }
    }

}
