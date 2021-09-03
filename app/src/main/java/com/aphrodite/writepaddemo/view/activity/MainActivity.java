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
    private CountDownTimer mCountDownTimer;

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
        mJQDCanvas.setLineWidth((float) 4.0);
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
        mPathDerive.setPathWidth(4);
        mPathDerive.setFps(10);
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
        Log.i(TAG, "点长度：" + elements.size());
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
        if (null != mCountDownTimer) {
            mCountDownTimer.cancel();
        }
        mJQDCanvas.post(new Runnable() {
            @Override
            public void run() {
                mCountDownTimer = new CountDownTimer(30 * 1000, 1) {
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
        map.put(PdfImpl.ParamsKey.TEXT_SIZE, 10);
        map.put(PdfImpl.ParamsKey.TEXT_COLOR, Color.BLACK);
        map.put(PdfImpl.ParamsKey.TYPEFACE, typeface);
        map.put(PdfImpl.ParamsKey.WIDTH, 320);
        map.put(PdfImpl.ParamsKey.HEIGHT, 2000);
        map.put(PdfImpl.ParamsKey.MARGIN_HORIZONTAL, 20);
        map.put(PdfImpl.ParamsKey.MARGIN_VERTICAL, 20);
        String text = "Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 " +
                "Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello " +
                "阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加 Hello 阿拉斯加基本概念\n" +
                "HTTP（HyperText Transfer Protocol：超文本传输协议）是一种用于分布式、协作式和超媒体信息系统的应用层协议。 简单来说就是一种发布和接收 HTML 页面的方法，被用于在 Web 浏览器和网站服务器之间传递信息。\n" +
                "\n" +
                "HTTP 默认工作在 TCP 协议 80 端口，用户访问网站 http:// 打头的都是标准 HTTP 服务。\n" +
                "\n" +
                "HTTP 协议以明文方式发送内容，不提供任何方式的数据加密，如果攻击者截取了Web浏览器和网站服务器之间的传输报文，就可以直接读懂其中的信息，因此，HTTP协议不适合传输一些敏感信息，比如：信用卡号、密码等支付信息。\n" +
                "\n" +
                "HTTPS（Hypertext Transfer Protocol Secure：超文本传输安全协议）是一种透过计算机网络进行安全通信的传输协议。HTTPS 经由 HTTP 进行通信，但利用 SSL/TLS 来加密数据包。HTTPS 开发的主要目的，是提供对网站服务器的身份认证，保护交换数据的隐私与完整性。\n" +
                "\n" +
                "HTTPS 默认工作在 TCP 协议443端口，它的工作流程一般如以下方式：\n" +
                "\n" +
                "1、TCP 三次同步握手\n" +
                "2、客户端验证服务器数字证书\n" +
                "3、DH 算法协商对称加密算法的密钥、hash 算法的密钥\n" +
                "4、SSL 安全加密隧道协商完成\n" +
                "5、网页以加密的方式传输，用协商的对称加密算法和密钥加密，保证数据机密性；用协商的hash算法进行数据完整性保护，保证数据不被篡改。\n" +
                "截至 2018 年 6 月，Alexa 排名前 100 万的网站中有 34.6% 使用 HTTPS 作为默认值，互联网 141387 个最受欢迎网站的 43.1% 具有安全实施的 HTTPS，以及 45% 的页面加载（透过Firefox纪录）使用HTTPS。2017 年3 月，中国注册域名总数的 0.11％使用 HTTPS。\n" +
                "\n" +
                "根据 Mozilla 统计，自 2017 年 1 月以来，超过一半的网站流量被加密。\n" +
                "\n" +
                "HTTP 与 HTTPS 区别\n" +
                "HTTP 明文传输，数据都是未加密的，安全性较差，HTTPS（SSL+HTTP） 数据传输过程是加密的，安全性较好。\n" +
                "使用 HTTPS 协议需要到 CA（Certificate Authority，数字证书认证机构） 申请证书，一般免费证书较少，因而需要一定费用。证书颁发机构如：Symantec、Comodo、GoDaddy 和 GlobalSign 等。\n" +
                "HTTP 页面响应速度比 HTTPS 快，主要是因为 HTTP 使用 TCP 三次握手建立连接，客户端和服务器需要交换 3 个包，而 HTTPS除了 TCP 的三个包，还要加上 ssl 握手需要的 9 个包，所以一共是 12 个包。\n" +
                "http 和 https 使用的是完全不同的连接方式，用的端口也不一样，前者是 80，后者是 443。\n" +
                "HTTPS 其实就是建构在 SSL/TLS 之上的 HTTP 协议，所以，要比较 HTTPS 比 HTTP 要更耗费服务器资源。20210903";
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