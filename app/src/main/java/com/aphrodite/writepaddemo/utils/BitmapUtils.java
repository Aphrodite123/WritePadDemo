package com.aphrodite.writepaddemo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aphrodite.framework.utils.PathUtils;
import com.aphrodite.writepaddemo.config.AppConfig;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * bitmap工具类：文字转成图片
 * Created by frank on 2018/1/24.
 */

public class BitmapUtils {
    private final static int TEXT_SIZE = 16;
    private final static int TEXT_COLOR = Color.RED;

    /**
     * 文本转成Bitmap
     *
     * @param text    文本内容
     * @param context 上下文
     * @return 图片的bitmap
     */
    private static Bitmap textToBitmap(String text, Context context) {
        float scale = context.getResources().getDisplayMetrics().scaledDensity;
        TextView tv = new TextView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(layoutParams);
        tv.setText(text);
        tv.setTextSize(scale * TEXT_SIZE);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setDrawingCacheEnabled(true);
        tv.setTextColor(TEXT_COLOR);
        tv.setBackgroundColor(Color.WHITE);
        tv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());
        tv.buildDrawingCache();
        Bitmap bitmap = tv.getDrawingCache();
        int rate = bitmap.getHeight() / 20;
        return Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / rate, 20, false);
    }

    /**
     * 文字生成图片
     *
     * @param filePath filePath
     * @param text     text
     * @param context  context
     * @return 生成图片是否成功
     */
    public static boolean textToPicture(String filePath, String text, Context context) {
        Bitmap bitmap = textToBitmap(text, context);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 删除源文件
     *
     * @param filePath filePath
     * @return 删除是否成功
     */
    public static boolean deleteTextFile(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    /**
     * 将Bitmap保存到本地路径
     *
     * @param bitmap
     * @param absolutePath
     * @param format
     * @param quality
     * @throws IOException
     */
    public static String saveBitmap(Bitmap bitmap, String absolutePath, Bitmap.CompressFormat format, int quality) throws IOException {
        if (null == bitmap || TextUtils.isEmpty(absolutePath)) {
            return null;
        }
        String path = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator) + 1);
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        String resFileName = PathUtils.getFileNameFromUrl(absolutePath);
        String curFileName = null;
        if (TextUtils.isEmpty(resFileName)) {
            curFileName = System.currentTimeMillis() + AppConfig.POINT + format.name();
        } else {
            curFileName = resFileName;
        }
        File saveFile = new File(path, curFileName);
        if (!saveFile.exists()) {
            saveFile.createNewFile();
        } else {
            if (saveFile.delete()) {
                saveFile.createNewFile();
            }
        }

        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(saveFile));
        bitmap.compress(format, quality, outputStream);
        outputStream.flush();
        outputStream.close();
        return path + curFileName;
    }

    /**
     * 将Bitmap保存到本地路径
     *
     * @param bitmap
     * @param path
     * @param fileName
     * @throws IOException
     */
    public static String saveBitmap(Bitmap bitmap, String path, String fileName, Bitmap.CompressFormat format, int quality) throws IOException {
        if (null == bitmap || TextUtils.isEmpty(path) || TextUtils.isEmpty(fileName)) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        File saveFile = new File(path, fileName);
        if (!saveFile.exists()) {
            saveFile.createNewFile();
        } else {
            if (saveFile.delete()) {
                saveFile.createNewFile();
            }
        }

        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(saveFile));
        bitmap.compress(format, quality, outputStream);
        outputStream.flush();
        outputStream.close();
        return path + fileName;
    }

    /**
     * 给指定到View截图
     *
     * @param view
     * @return
     */
    public static Bitmap shotToView(View view) {
        if (null == view) {
            return null;
        }
        view.buildDrawingCache();
        return view.getDrawingCache();
    }

    /**
     * 设置Bitmap背景色
     *
     * @param color
     * @param orginBitmap
     * @return
     */
    public static Bitmap drawBitmapBgColor(int color, Bitmap orginBitmap) {
        Paint paint = new Paint();
        paint.setColor(color);
        Bitmap bitmap = Bitmap.createBitmap(orginBitmap.getWidth(), orginBitmap.getHeight(), orginBitmap.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, orginBitmap.getWidth(), orginBitmap.getHeight(), paint);
        canvas.drawBitmap(orginBitmap, 0, 0, paint);
        return bitmap;
    }

}
