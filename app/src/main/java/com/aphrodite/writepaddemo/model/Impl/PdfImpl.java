package com.aphrodite.writepaddemo.model.Impl;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import com.aphrodite.writepaddemo.utils.UIUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Aphrodite on 2021/3/10.
 * 使用PdfDocument将Text/Image按要求转PDF
 */
public class PdfImpl {
    private String mPath;
    private int mWidth;
    private int mHeight;
    private int mColor;
    private int mFontSize;
    private int marginHorizontal;
    private int marginVertical;
    private Typeface mTypeface;

    private PdfDocument mDocument;
    private PdfDocument.PageInfo mPageInfo;
    private int mCanvasWidth;
    private int mCanvasHeight;
    private TextPaint mTextPaint;

    public PdfImpl(Builder builder) {
        this.mPath = builder.mPath;
        this.mWidth = builder.mWidth;
        this.mHeight = builder.mHeight;
        this.mColor = builder.mColor;
        this.mFontSize = builder.mFontSize;
        this.marginHorizontal = builder.marginHorizontal;
        this.marginVertical = builder.marginVertical;
        this.mTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
    }

    public PdfImpl init() {
        // create a new document
        mDocument = new PdfDocument();
        // create a page description
        mCanvasWidth = mWidth - 2 * marginHorizontal;
        mCanvasHeight = mHeight - 2 * marginVertical;
        Rect rect = new Rect(marginHorizontal, marginVertical, mWidth - marginHorizontal, mHeight - marginVertical);
        PdfDocument.PageInfo.Builder builder = new PdfDocument.PageInfo.Builder(mWidth, mHeight, 1);
        if (null != rect) {
            builder.setContentRect(rect);
        }
        mPageInfo = builder.create();
        return this;
    }

    public PdfImpl addImageToPdf(Bitmap bitmap) {
        if (null == bitmap) {
            return this;
        }
        // start a page
        PdfDocument.Page page = mDocument.startPage(mPageInfo);
        float scaleX = mCanvasWidth / (float) bitmap.getWidth();
        float scaleY = mCanvasHeight / (float) bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // draw something on the page
        page.getCanvas().drawBitmap(bitmap, matrix, paint);
        finishPage(page);
        return this;
    }

    public PdfImpl addTextToPdf(String content) {
        if (TextUtils.isEmpty(content) || null == mPageInfo) {
            return this;
        }
        mTextPaint = new TextPaint();
        mTextPaint.setColor(mColor);
        mTextPaint.setTextSize(mFontSize);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setTypeface(mTypeface);

        StaticLayout staticLayout = new StaticLayout(content, 0, content.length(), mTextPaint,
                mCanvasWidth, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false);
        int textHeight = staticLayout.getHeight();

        int currentHeight = 0;
        while (currentHeight <= textHeight) {
            int preLine = staticLayout.getLineForVertical(currentHeight);
            int nextLine = staticLayout.getLineForVertical(currentHeight += mCanvasHeight);
            newPage(content, preLine <= 0 ? 0 : staticLayout.getLineVisibleEnd(preLine), staticLayout.getLineVisibleEnd(nextLine));
        }
        return this;
    }

    private void newPage(String content, int prePosition, int nextPosition) {
        if (null == mDocument || null == mPageInfo) {
            return;
        }
        PdfDocument.Page page = mDocument.startPage(mPageInfo);
        StaticLayout staticLayout = new StaticLayout(content, prePosition, nextPosition,
                mTextPaint, mCanvasWidth, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false);
        staticLayout.draw(page.getCanvas());
        finishPage(page);
    }

    public PdfImpl finishPage(PdfDocument.Page page) {
        if (null != mDocument) {
            // finish the page
            mDocument.finishPage(page);
        }
        return this;
    }

    public boolean save() {
        if (TextUtils.isEmpty(mPath) || null == mDocument) {
            return false;
        }
        File saveFile = new File(mPath);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(saveFile);
            // write the document content
            mDocument.writeTo(outputStream);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // close the document
            mDocument.close();
            mDocument = null;
        }
        return false;
    }

    public static class Builder {
        private String mPath;
        private int mWidth;
        private int mHeight;
        private int mColor;
        private int mFontSize;
        private int marginHorizontal;
        private int marginVertical;

        public Builder(String path, Map<String, Object> params) {
            this.mPath = path;
            this.initData(params);
        }

        private void initData(Map<String, Object> params) {
            if (null == params || params.size() <= 0) {
                return;
            }
            mColor = params.containsKey(ParamsKey.COLOR) ?
                    Color.parseColor((String) params.get(ParamsKey.COLOR)) : Color.BLACK;
            mFontSize = params.containsKey(ParamsKey.FONT_SIZE) ?
                    new Double((Double) params.get(ParamsKey.FONT_SIZE)).intValue() : 10;
            marginHorizontal = params.containsKey(ParamsKey.MARGIN_HORIZONTAL) ?
                    new Double((Double) params.get(ParamsKey.MARGIN_HORIZONTAL)).intValue() : 20;
            marginVertical = params.containsKey(ParamsKey.MARGIN_VERTICAL) ?
                    new Double((Double) params.get(ParamsKey.MARGIN_VERTICAL)).intValue() : 20;
            Map<String, Object> pageSize = params.containsKey(ParamsKey.PAGE_SIZE) ?
                    (Map<String, Object>) params.get(ParamsKey.PAGE_SIZE) : null;
            if (null != pageSize && pageSize.size() > 0) {
                mWidth = pageSize.containsKey(ParamsKey.WIDTH) ?
                        new Double((Double) pageSize.get(ParamsKey.WIDTH)).intValue() : 0;
                mHeight = pageSize.containsKey(ParamsKey.HEIGHT) ?
                        new Double((Double) pageSize.get(ParamsKey.HEIGHT)).intValue() : 0;
            }
        }

        public Builder setColor(int color) {
            this.mColor = color;
            return this;
        }

        public Builder setFontSize(int fontSize) {
            this.mFontSize = fontSize;
            return this;
        }

        public Builder setMarginHorizontal(int marginHorizontal) {
            this.marginHorizontal = marginHorizontal;
            return this;
        }

        public Builder setMarginVertical(int marginVertical) {
            this.marginVertical = marginVertical;
            return this;
        }

        public Builder setPageSize(Map<String, Object> pageSize) {
            if (null == pageSize || pageSize.size() <= 0) {
                return this;
            }
            this.mWidth = Integer.parseInt(pageSize.get(ParamsKey.WIDTH).toString());
            this.mHeight = Integer.parseInt(pageSize.get(ParamsKey.HEIGHT).toString());
            return this;
        }

        public PdfImpl build() {
            return new PdfImpl(this);
        }

    }

    public interface ParamsKey {
        String COLOR = "color";
        String FONT_SIZE = "fontSize";
        String MARGIN_HORIZONTAL = "marginHorizontal";
        String MARGIN_VERTICAL = "marginVertical";
        String PAGE_SIZE = "pageSize";
        String WIDTH = "width";
        String HEIGHT = "height";
    }

}
