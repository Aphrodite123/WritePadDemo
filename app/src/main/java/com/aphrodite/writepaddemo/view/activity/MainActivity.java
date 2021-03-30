package com.aphrodite.writepaddemo.view.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aphrodite.writepaddemo.R;
import com.aphrodite.writepaddemo.config.AppConfig;
import com.aphrodite.writepaddemo.model.Impl.JQDPainter;
import com.aphrodite.writepaddemo.model.Impl.PdfImpl;
import com.aphrodite.writepaddemo.model.api.IPathCallBack;
import com.aphrodite.writepaddemo.model.bean.PointBean;
import com.aphrodite.writepaddemo.model.bean.PointsBean;
import com.aphrodite.writepaddemo.utils.PathUtils;
import com.aphrodite.writepaddemo.view.base.BaseActivity;
import com.aphrodite.writepaddemo.view.widget.view.JQDCanvas;
import com.google.gson.Gson;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;

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

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private LinearLayout mRoot;
    private JQDCanvas mJQDCanvas;
    private Button mGainPicture;
    private Button mGainVideo;
    private Button mGainPdf;
    private Button mUndo;
    private Button mSaveImage;
    private Button mTextPdf;
    private TextView mContent;

    private static final String TAG = MainActivity.class.getSimpleName();
    //生成图片点间隔数，默认：5
    private static int DEFAULT_IMAGE_INTERVAL = 5;
    private int mCount = 0;

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

    @Override
    protected int getViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        mRoot = findViewById(R.id.root);
        mJQDCanvas = findViewById(R.id.pen);
        mGainPicture = findViewById(R.id.gain_picture);
        mGainVideo = findViewById(R.id.gain_video);
        mGainPdf = findViewById(R.id.gain_pdf);
        mUndo = findViewById(R.id.undo);
        mSaveImage = findViewById(R.id.save_image);
        mTextPdf = findViewById(R.id.text_trans_pdf);
        mContent = findViewById(R.id.content);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
    }

    @Override
    protected void initListener() {
        mGainPicture.setOnClickListener(this::onClick);
        mGainVideo.setOnClickListener(this::onClick);
        mGainPdf.setOnClickListener(this::onClick);
        mUndo.setOnClickListener(this::onClick);
        mSaveImage.setOnClickListener(this::onClick);
        mTextPdf.setOnClickListener(this::onClick);
    }

    @Override
    protected void initData() {
        Typeface typeface = Typeface.DEFAULT;
        Log.i(TAG, "Style of font. " + typeface.getStyle());

        //路径：/storage/emulated/0/Android/data/com.aphrodite.writepaddemo/files/，注：米家插件则为沙盒目录
        mRootPath = PathUtils.getExternalFileDir(this) + "/202103051536/";
        mGson = new Gson();
        //JQDCanvas设置
        mJQDCanvas.init(mRootPath);
        mJQDCanvas.setScale((float) (getDensity() * 0.03));
        //文件设置
        if (!hasPermission(mPermissions)) {
            requestPermission(mPermissions, AppConfig.PermissionType.CAMERA_PERMISSION);
        }
        createFile();
        //JQDPainter设置
        mPathDerive = JQDPainter.getInstance(this);
        mPathDerive.init(mRootPath);
        mPathDerive.setScale((float) (getDensity() * 0.03));
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
        List<UgeePoint> uptimizedPoints = new ArrayList<>();
        List<UgeePoint> ugeePoints = new ArrayList<>();
        mUgeePenOptimizeClass = new UgeePenOptimizeClass(new cn.ugee.mi.optimize.OnPenCallBack() {
            @Override
            public void onPenOptimizeDate(UgeePoint ugeePoint) {
                if (null == ugeePoint) {
                    return;
                }
                uptimizedPoints.add(ugeePoint);

                mJQDCanvas.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCount < 5) {
                            ugeePoints.add(ugeePoint);
                            mCount++;
                        } else {
                            mJQDCanvas.displayPoints(ugeePoints);
                            mCount = 0;
                            ugeePoints.clear();
                        }
                    }
                });
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
        byte state = 0;
        for (PointBean bean : pointBeans) {
            if (null == bean) {
                continue;
            }
            //0-悬浮；1-书写；2-离开
            switch (bean.getState()) {
                case 160:
                    state = 0;
                    break;
                case 161:
                    state = 1;
                    break;
                default:
                    break;
            }
            element = new UgeePoint(bean.getX(), bean.getY(), bean.getPressure(), state);
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

    private Font setFont(float size, int style, BaseColor color) {
        BaseFont baseFont = null;
        Font font = null;
        try {
            baseFont = BaseFont.createFont("/assets/fonts/simsun.ttc,1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            font = new Font(baseFont, size, style, color);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return font;
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gain_picture:
                //获取图片
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
                break;
            case R.id.gain_video:
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
                break;
            case R.id.gain_pdf:
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
                break;
            case R.id.undo:
                mJQDCanvas.revoke();
                break;
            case R.id.save_image:
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
                break;
            case R.id.text_trans_pdf:
                Map<String, Object> map = new HashMap<>();
                Font textFont = setFont(16f, Font.NORMAL, new BaseColor(Color.BLACK));
                map.put(PdfImpl.ParamsKey.FONT, textFont);
                map.put(PdfImpl.ParamsKey.WIDTH, 1080);
                map.put(PdfImpl.ParamsKey.HEIGHT, 1920);
                map.put(PdfImpl.ParamsKey.MARGIN_HORIZONTAL, 20);
                map.put(PdfImpl.ParamsKey.MARGIN_VERTICAL, 20);
                mPathDerive.createPDFWithText("Hello 阿拉斯加", "202103051538.pdf", map,
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
                break;
        }
    }

}