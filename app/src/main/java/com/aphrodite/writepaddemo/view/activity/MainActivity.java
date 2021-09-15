package com.aphrodite.writepaddemo.view.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aphrodite.writepaddemo.R;
import com.aphrodite.writepaddemo.config.AppConfig;
import com.aphrodite.writepaddemo.model.Impl.JQDPainter;
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
import cn.ugee.mi.optimize.UgeePoint;

public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private JQDCanvas mJQDCanvas;
    private TextView mContent;

    private String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA};
    private String mRootPath;

    private JQDPainter mPathDerive;
    private int mIndex = 0;
    private CountDownTimer mCountDownTimer;

    private List<UgeePoint> mSourcePoints;

    private String mConfig = "{\"backgroundColor\": \"white\", \"fps\": 4, \"lineColor\": " +
            "\"black\", \"lineWidth\": 1.5, \"maxPressure\": 2047, \"pointsPerFrame\": 40, \"scale\": 0.01, \"size\": {\"height\": 27400, \"width\": 20600}}";
    private String mPdfConfig = "{\"color\": \"#000000\", \"fontSize\": 10, \"marginHorizontal\": 20, \"marginVertical\": 20, \"pageSize\": {\"height\": 274, \"width\": 206}}";
    private String mPdfText = "证券时报记者 谢忠翔\n" +
            "随着A股上市银行半年报披露完毕，各银行上半年涉房贷款占比也逐一显现。\n" +
            "证券时报记者梳理41家A股上市银行披露的涉房贷款比例数据后了解到，目前仍有11家银行的个人住房贷款占比超出监管要求，10家银行的房地产业贷款超出“红线”。另一方面，上市银行涉房贷款的不良贷款率也明显上升，资产质量承压。\n" +
            "同时，在A股上市银行中，多数满足监管要求，但不少银行仍选择对房地产企业贷款进一步压降；而个人住房贷款方面则有所分化，部分银行忙着降比例，也有部分银行个人房贷占比明显上升。\n" +
            "分析人士表示，国内结构性房地产调控趋严的情况下，银行积极落实房贷“两道红线”监管，并且对房企信贷风险进一步防范。同时，仍有部分银行存在较大整改压力，由于监管规定了2~4年的过渡期，预计绝大部分银行能够完成整改。\n" +
            "多家银行涉房贷踩红线\n" +
            "9月3日，央行发布的《中国金融稳定报告（2021）》称，当前，房地产贷款集中度管理制度已进入常态化实施阶段。\n" +
            "去年末，央行、银保监会宣布实施房地产贷款集中度管理制度，对银行业金融机构设置房地产贷款占比和个人住房贷款占比“两道红线”，并对不同体量的银行设置了五档标准。\n" +
            "在个人住房贷款占比方面，五个档次的占比上限分别为32.5%、20%、17.5%、12.5%、7.5%。\n" +
            "证券时报记者注意到，相较去年末，仍有多家银行的涉房贷款比例超出监管规定的“两道红线”。在个人住房贷款占比方面，建设银行、邮储银行、兴业银行、招商银行等11家银行超出监管上限，但总体均较去年末有不同幅度的压降。\n" +
            "例如，建设银行的个人房贷占比从年初的34.73%下降至33.72%；邮储银行较年初下降0.65个百分点；兴业银行25.94%的数据也较年初下降了0.61个百分点，不过仍比监管要求高出5.94个百分点；招商银行的这一数据为24.71%，较红线要求高出4.71个百分点。\n" +
            "在房地产业贷款占比方面，监管对五个档次给出的上限标准分别为40%、27.5%、22.5%、17.5%、12.5%。据A股上市银行的半年报，目前仍有10家银行处于“踩线”状态。\n" +
            "与个人房贷占比超标类似的是，兴业银行、招商银行和成都银行等10家机构，均相对较多地超出监管标准，兴业银行、招行、成都银行的房地产贷款占比分别为34.61%、32.22%、29.44%，分别超出7.11、4.72、6.94个百分点，在满足监管要求的整改上存在相对较大的压力。\n" +
            "兴业银行在半年报中称，“公司将主动适应更加严格和精细的房地产调控政策，按照监管部门的房地产贷款集中度管理方案稳健投放房地产信贷业务。”招行也在半年报中表示，将继续加强房地产贷款集中度管理，推动房地产贷款占比稳步下降，预计房地产贷款集中度管理政策的总体影响可控。\n" +
            "对于仍有不少银行涉房贷款超标的原因，光大银行金融业分析师周茂华认为，一是历史存量较大；二是个人按揭房贷在银行眼里仍是“优质资产”，同时由于担忧客户流失，部分银行压降积极性不高。\n" +
            "“从目前触及监管‘红线’的银行看，占比超限程度不严重，同时国内强化监管，预计过渡期内绝大多数银行还是能完成整改的。”周茂华称。\n" +
            "房地产贷款比例“瘦身”\n" +
            "为符合监管要求，今年上半年，不少银行纷纷压降涉房贷款比例。证券时报记者梳理A股41家上市银行的半年报发现，除上海农商行、紫金银行未披露外，共有27家银行的房地产贷款较去年末有所下降。\n" +
            "其中，六大国有银行的房地产贷款比例均较年初有所下降，中国银行和建设银行分别降低了1.34、1.01个百分点。城商行中，压降幅度较大的分别为成都银行、杭州银行、青岛银行等，分别较年初下降了5.22、2.38、1.47个百分点。\n" +
            "不过，在个人住房贷款这项数据上，由于不同上市银行距离监管上限的空间并不一致，一些接近监管红线的银行忙着压降个人住房贷款，而另一些个人房贷占比低的银行却在加大对个人房贷的投放。\n" +
            "数据显示，有20家上市银行的这一指标有所压降，另外19家银行的个人贷款占比较年初上升了0.03~7.06个百分点。其中，杭州银行目前个人住房贷款占比达14.32%，较年初的7.26%提升了7.06个百分点，在A股银行中上升最快。\n" +
            "不过，也有一些银行虽然个人住房贷款占比大幅低于同业，但目前仍选择“按兵不动”。例如，平安银行和浙商银行的这一占比仅分别为9.01%、7.03%，较年初分别提升了0.03、0.18个百分点。\n" +
            "平安银行副行长郭世邦在半年报业绩发布会上表示，“我们的两个指标距离监管要求还很远，但这并不代表我们就有机会，并不是说马上就能投放很多房地产贷款，还是要按照监管要求来。”无独有偶，各大银行召开的业绩发布中，不少银行高层表示将继续按监管要求，严格控制好房地产贷款的规模和占比。";

    @Override
    protected int getViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        mJQDCanvas = findViewById(R.id.pen);
        mContent = findViewById(R.id.content);
    }

    @Override
    protected void initListener() {
    }

    @Override
    protected void initData() {
        //读取原始数据
        mSourcePoints = new ArrayList<>();
        mSourcePoints.addAll(processPoints());

        //路径：/storage/emulated/0/Android/data/com.aphrodite.writepaddemo/files/，注：米家插件则为沙盒目录
        mRootPath = PathUtils.getExternalFileDir(this) + "/20210914/";
        //JQDCanvas设置
        mJQDCanvas.init(mRootPath);
        mJQDCanvas.setScale(this, 0.01f);
        mJQDCanvas.setLineWidth((float) 4.0);
        //文件设置
        if (hasPermission(mPermissions)) {
            createFile();
        } else {
            requestPermission(mPermissions, AppConfig.PermissionType.WRITE_EXTERNAL_PERMISSION);
        }
        //JQDPainter设置
        mPathDerive = new JQDPainter(this);
        mPathDerive.init(mRootPath);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case AppConfig.PermissionType.WRITE_EXTERNAL_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createFile();
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

    private List<UgeePoint> processPoints() {
        Gson gson = new Gson();
        PointsBean pointsBean = gson.fromJson(readPoints(), PointsBean.class);
        List<PointBean> pointBeans = pointsBean.getData();
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
        if (null != mCountDownTimer) {
            mCountDownTimer.cancel();
        }
        mJQDCanvas.post(new Runnable() {
            @Override
            public void run() {
                mCountDownTimer = new CountDownTimer(30 * 1000, 1) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (mIndex <= mSourcePoints.size() - 5) {
                            mJQDCanvas.displayPoints(mSourcePoints.subList(mIndex, mIndex + 5));
                            mIndex += 5;
                        } else {
                            if (mIndex < mSourcePoints.size()) {
                                mJQDCanvas.displayPoints(mSourcePoints.subList(mIndex, mSourcePoints.size()));
                                mIndex = mSourcePoints.size();
                            }
                        }
                    }

                    @Override
                    public void onFinish() {
                        mIndex = 0;
                    }
                };
                mCountDownTimer.start();
            }
        });
    }

    public void start(View start) {
        if (null == mCountDownTimer) {
            return;
        }
        mCountDownTimer.start();
    }

    public void pause(View pause) {
        if (null == mCountDownTimer) {
            return;
        }
        mCountDownTimer.cancel();
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
        mCountDownTimer.cancel();
        mJQDCanvas.clear();
    }

    private Map<String, Object> getParams(String config) {
        if (TextUtils.isEmpty(config)) {
            return null;
        }
        Gson gson = new Gson();
        Map<String, Object> hashMap = new HashMap<>();
        hashMap = gson.fromJson(config, hashMap.getClass());
        return hashMap;
    }

    /**
     * 路径转图片
     */
    public void pathToImage(View pathToImage) {
        String fileName = System.currentTimeMillis() + ".png";
        mPathDerive.createMediaWithPoints(mSourcePoints, JQDPainter.Type.IMAGE, fileName, getParams(mConfig), new IPathCallBack() {
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
        String fileName = System.currentTimeMillis() + ".mp4";
        mPathDerive.createMediaWithPoints(mSourcePoints, JQDPainter.Type.VIDEO, fileName, getParams(mConfig), new IPathCallBack() {
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
        String fileName = System.currentTimeMillis() + ".pdf";
        mPathDerive.createMediaWithPoints(mSourcePoints, JQDPainter.Type.PDF, fileName, getParams(mConfig), new IPathCallBack() {
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
        mPathDerive.createPDFWithText(mPdfText, "202103051538.pdf", getParams(mPdfConfig), new IPathCallBack() {
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