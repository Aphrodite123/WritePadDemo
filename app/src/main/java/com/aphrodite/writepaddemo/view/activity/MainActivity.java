package com.aphrodite.writepaddemo.view.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aphrodite.writepaddemo.R;
import com.aphrodite.writepaddemo.config.AppConfig;
import com.aphrodite.writepaddemo.model.Impl.JQDPainter;
import com.aphrodite.writepaddemo.model.Impl.PdfImpl;
import com.aphrodite.writepaddemo.model.api.IPathCallBack;
import com.aphrodite.writepaddemo.model.bean.PointBean;
import com.aphrodite.writepaddemo.model.bean.PointsBean;
import com.aphrodite.writepaddemo.utils.FileUtils;
import com.aphrodite.writepaddemo.utils.PathUtils;
import com.aphrodite.writepaddemo.view.base.BaseActivity;
import com.aphrodite.writepaddemo.view.widget.view.JQDCanvas;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import cn.ugee.mi.optimize.UgeePenOptimizeClass;
import cn.ugee.mi.optimize.UgeePoint;

public class MainActivity extends BaseActivity {
    private LinearLayout mRoot;
    private JQDCanvas mJQDCanvas;
    private TextView mContent;

    private static final String TAG = MainActivity.class.getSimpleName();
    //生成图片点间隔数，默认：5
    private static int DEFAULT_IMAGE_INTERVAL = 40;

    private String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA};
    private String mRootPath;
    private PointsBean mPointsBean;
    private UgeePenOptimizeClass mUgeePenOptimizeClass;
    private Gson mGson;

    private JQDPainter mPathDerive;
    private List<UgeePoint> uptimizedPoints = new ArrayList<>();
    private int mIndex = 0;

    @Override
    protected int getViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        mRoot = findViewById(R.id.root);
        mJQDCanvas = findViewById(R.id.pen);
        mContent = findViewById(R.id.content);
    }

    @Override
    protected void initListener() {
    }

    @Override
    protected void initData() {
        //路径：/storage/emulated/0/Android/data/com.aphrodite.writepaddemo/files/，注：米家插件则为沙盒目录
        mRootPath = PathUtils.getExternalFileDir(this) + "/202103051536/";
        mGson = new Gson();
        //JQDCanvas设置
        mJQDCanvas.init(mRootPath);
        mJQDCanvas.setScale(this, 0.01f);
        mJQDCanvas.setLineWidth((float) 10.0);
        //文件设置
        if (!hasPermission(mPermissions)) {
            requestPermission(mPermissions, AppConfig.PermissionType.CAMERA_PERMISSION);
        }
        createFile();
        //JQDPainter设置
        mPathDerive = JQDPainter.getInstance(this);
        mPathDerive.init(mRootPath);
        mPathDerive.setScale(this, 0.01f);
        mPathDerive.setImageBgColor(Color.WHITE);
        mPathDerive.setPathColor(Color.BLACK);
        mPathDerive.setPathWidth(10);
        //点优化
        optimizePoints();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case AppConfig.PermissionType.CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createFile();
                    optimizePoints();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mPathDerive) {
            mPathDerive.destroy();
        }
    }

    private void createFile() {
        File file = new File(mRootPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    private void optimizePoints() {
        String line = readPoints();
        if (TextUtils.isEmpty(line)) {
            return;
        }
        mPointsBean = mGson.fromJson(line, PointsBean.class);
        List<UgeePoint> elements = processPoints();
        mUgeePenOptimizeClass = new UgeePenOptimizeClass(new cn.ugee.mi.optimize.OnPenCallBack() {
            @Override
            public void onPenOptimizeDate(UgeePoint ugeePoint) {
                if (null == ugeePoint) {
                    return;
                }
                uptimizedPoints.add(ugeePoint);
            }

            @Override
            public void onCompleteDate(boolean b) {
                if (b) {
                    mPathDerive.addPoints(uptimizedPoints);
                }
            }
        });
        //轨迹数据集合优化
        mUgeePenOptimizeClass.customListPoint(elements);
    }

    private List<UgeePoint> processPoints() {
        if (null == mPointsBean) {
            return null;
        }
        List<PointBean> pointBeans = mPointsBean.getData();
        if (null == pointBeans || pointBeans.size() <= 0) {
            return null;
        }
        List<UgeePoint> elements = new ArrayList<>();
        UgeePoint element = null;
        for (PointBean bean : pointBeans) {
            if (null == bean) {
                continue;
            }
            element = new UgeePoint(bean.getX(), bean.getY(), bean.getPressure(), bean.getState());
            elements.add(element);
        }
        return elements;
    }

    private String readPoints() {
        AssetManager am = getAssets();
        InputStream is = null;
        try {
            is = am.open("210301101231.txt");
            String code = getCode(is);
            is = am.open("210301101231.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, code));
            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 在线轨迹转图片
     */
    public void transImageOnline(View transImageOnline) {
        mJQDCanvas.post(new Runnable() {
            @Override
            public void run() {
                mJQDCanvas.createImageWithFilename("202103051538.png", new IPathCallBack() {
                    @Override
                    public void success(String path) {
                        Log.i(TAG, "Path of current image: " + path);
                        if (null != mContent) {
                            mContent.setText("实时图片路径为：" + path);
                        }
                    }

                    @Override
                    public void failed(int code) {
                        Log.e(TAG, "Create code failed." + code);
                    }
                });
            }
        });
    }

    /**
     * 展示
     */
    public void show(View show) {
        mJQDCanvas.clear();
        mJQDCanvas.setImageBgColor(Color.WHITE);
        mJQDCanvas.setLineColor(Color.BLACK);
        mJQDCanvas.post(new Runnable() {
            @Override
            public void run() {
                CountDownTimer countDownTimer = new CountDownTimer(50 * 1000, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (mIndex <= uptimizedPoints.size() - 5) {
                            mJQDCanvas.displayPoints(uptimizedPoints.subList(mIndex, mIndex + 5));
                            mIndex += 5;
                        } else {
                            if (mIndex < uptimizedPoints.size()) {
                                mJQDCanvas.displayPoints(uptimizedPoints.subList(mIndex, uptimizedPoints.size()));
                                mIndex = uptimizedPoints.size();
                            }
                        }
                    }

                    @Override
                    public void onFinish() {
                        mIndex = 0;
                    }
                };
                countDownTimer.start();
            }
        });
    }

    /**
     * 是否可撤销
     */
    public void canRevoke(View canRevoke) {
        if (null != mContent) {
            mContent.setText("是否可以进行撤销：" + mJQDCanvas.canRevoke());
        }
        Log.i(TAG, "Revoke status. " + mJQDCanvas.canRevoke());
    }

    /**
     * 撤销
     */
    public void revoke(View revoke) {
        mJQDCanvas.revoke();
    }

    /**
     * 清除
     */
    public void clear(View clear) {
        mJQDCanvas.clear();
    }

    /**
     * 路径转图片
     */
    public void pathToImage(View pathToImage) {
        mPathDerive.createImageWithPoints("202103051538.png", new IPathCallBack() {
            @Override
            public void success(String path) {
                Log.i(TAG, "Path of picture: " + path);
                if (null != mContent) {
                    mContent.setText("图片路径为：" + path);
                }
            }

            @Override
            public void failed(int code) {
                Log.e(TAG, "Create image failed." + code);
            }
        });
    }

    /**
     * 路径转视频
     */
    public void pathToVideo(View pathToVideo) {
        Log.i(TAG, "Start path to video.");
        mPathDerive.createVideoWithPoints("202103051538.mp4", DEFAULT_IMAGE_INTERVAL, new IPathCallBack() {
            @Override
            public void success(String path) {
                Log.i(TAG, "Path of video: " + path);
                if (null != mContent) {
                    mContent.setText("Video路径为：" + path);
                }
            }

            @Override
            public void failed(int code) {
                Log.e(TAG, "Create video failed." + code);
            }
        });
    }

    /**
     * 路径转PDF
     */
    public void pathToPdf(View pathToPdf) {
        mPathDerive.createPDFWithPoints("202103051538.pdf", new IPathCallBack() {
            @Override
            public void success(String path) {
                Log.i(TAG, "Path of pdf: " + path);
                if (null != mContent) {
                    mContent.setText("Pdf路径为：" + path);
                }
            }

            @Override
            public void failed(int code) {
                Log.e(TAG, "Create pdf failed." + code);
            }
        });
    }

    /**
     * Text转PDF
     */
    public void textToPdf(View textToPdf) {
        Map<String, Object> map = new HashMap<>();
        Typeface typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        map.put(PdfImpl.ParamsKey.TEXT_SIZE, 20);
        map.put(PdfImpl.ParamsKey.TEXT_COLOR, Color.BLACK);
        map.put(PdfImpl.ParamsKey.TYPEFACE, typeface);
        map.put(PdfImpl.ParamsKey.WIDTH, 320);
        map.put(PdfImpl.ParamsKey.HEIGHT, 260);
        map.put(PdfImpl.ParamsKey.MARGIN_HORIZONTAL, 20);
        map.put(PdfImpl.ParamsKey.MARGIN_VERTICAL, 20);
        String text = "Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 " +
                "Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello " +
                "阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加";
        mPathDerive.createPDFWithText(text, "202103051538.pdf", map,
                new IPathCallBack() {
                    @Override
                    public void success(String path) {
                        Log.i(TAG, "Path of pdf: " + path);
                        if (null != mContent) {
                            mContent.setText("Pdf路径为：" + path);
                        }
                    }

                    @Override
                    public void failed(int code) {
                        Log.e(TAG, "Create pdf failed." + code);
                    }
                });
    }

    /**
     * 删除已存在文件
     */
    public void delete(View delete) {
        FileUtils.deleteFile(mRootPath);
    }

    private String getCode(InputStream is) {
        try {
            BufferedInputStream bin = new BufferedInputStream(is);
            int p;
            p = (bin.read() << 8) + bin.read();
            String code = null;
            switch (p) {
                case 0xefbb:
                    code = "UTF-8";
                    break;
                case 0xfffe:
                    code = "Unicode";
                    break;
                case 0xfeff:
                    code = "UTF-16BE";
                    break;
                default:
                    code = "GBK";
            }
            is.close();
            return code;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private float getDensity() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.density;
    }

}