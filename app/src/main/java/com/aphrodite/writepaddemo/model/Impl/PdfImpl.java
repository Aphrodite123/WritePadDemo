package com.aphrodite.writepaddemo.model.Impl;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Aphrodite on 2021/3/10.
 * 使用iText讲文本和图片按要求转PDF
 */
public class PdfImpl {
    private String mPath;
    private int[] mPageSize;
    private int[] margins;
    private int mColor;
    private Typeface mTypeface;
    private float mTextSize;

    private PdfDocument mDocument;
    private PdfDocument.PageInfo mPageInfo;
    private PdfDocument.Page mPage;

    public PdfImpl(Builder builder) {
        this.mPath = builder.mPath;
        this.mPageSize = builder.mPageSize;
        this.margins = builder.margins;
        this.mColor = builder.mColor;
        this.mTypeface = builder.mTypeface;
        this.mTextSize = builder.mTextSize;
    }

    public PdfImpl init() {
        // create a new document
        mDocument = new PdfDocument();
        // create a page description
        Rect rect = null;
        if (null != margins && margins.length >= 4) {
            rect = new Rect(margins[0], margins[2], mPageSize[0] - margins[1], mPageSize[1] - margins[3]);
        }
        PdfDocument.PageInfo.Builder builder = new PdfDocument.PageInfo.Builder(mPageSize[0], mPageSize[1], 1);
        if (null != rect) {
            builder.setContentRect(rect);
        }
        mPageInfo = builder.create();
        // start a page
        mPage = mDocument.startPage(mPageInfo);
        return this;
    }

    public PdfImpl addImageToPdf(Bitmap bitmap) {
        if (null == bitmap || null == mPageSize || null == mPage) {
            return this;
        }
        float scale = mPageSize[0] / bitmap.getWidth();
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPage.getCanvas().drawBitmap(bitmap, matrix, paint);
        return this;
    }

    public PdfImpl addTextToPdf(String content) {
        if (TextUtils.isEmpty(content) || null == mPage) {
            return this;
        }
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(mColor);
        textPaint.setTextSize(mTextSize);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(mTypeface);

        StaticLayout staticLayout = new StaticLayout(content, 0, content.length(), textPaint, mPage.getCanvas().getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        staticLayout.draw(mPage.getCanvas());
        return this;
    }

    public PdfImpl finishPage() {
        if (null != mDocument) {
            mDocument.finishPage(mPage);
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
            mDocument.close();
            mDocument = null;
        }
        return false;
    }

    public static class Builder {
        private String mPath;
        private int[] mPageSize;
        private int[] margins;
        private float mTextSize;
        private int mColor;
        private Typeface mTypeface;

        public Builder(String path, int[] pageSize) {
            this.mPath = path;
            this.mPageSize = pageSize;
        }

        public Builder setMargins(int[] margins) {
            this.margins = margins;
            return this;
        }

        public Builder setTextSize(float textSize) {
            this.mTextSize = textSize;
            return this;
        }

        public Builder setColor(int color) {
            this.mColor = color;
            return this;
        }

        public Builder setTypeface(Typeface typeface) {
            this.mTypeface = typeface;
            return this;
        }

        public PdfImpl build() {
            return new PdfImpl(this);
        }

    }

    public interface ParamsKey {
        String TEXT_SIZE = "text_size";
        String TEXT_COLOR = "text_color";
        String TYPEFACE = "typeface";
        String WIDTH = "width";
        String HEIGHT = "height";
        String MARGIN_HORIZONTAL = "margin_horizontal";
        String MARGIN_VERTICAL = "margin_vertical";
    }

}
