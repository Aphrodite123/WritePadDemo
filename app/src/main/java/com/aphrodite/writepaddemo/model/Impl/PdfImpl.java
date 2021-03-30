package com.aphrodite.writepaddemo.model.Impl;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Aphrodite on 2021/3/10.
 * 使用iText讲文本和图片按要求转PDF
 */
public class PdfImpl {
    private String mPath;

    private Rectangle mPageSize;
    private float marginLeft;
    private float marginRight;
    private float marginTop;
    private float marginBottom;

    private Document mDocument;

    public PdfImpl(Builder builder) {
        this.mPath = builder.mPath;
        this.mPageSize = builder.mPageSize;
        this.marginLeft = builder.marginLeft;
        this.marginRight = builder.marginRight;
        this.marginTop = builder.marginTop;
        this.marginBottom = builder.marginBottom;
    }

    public PdfImpl init() throws FileNotFoundException, DocumentException {
        mDocument = new Document(mPageSize, marginLeft, marginRight, marginTop, marginBottom);
        PdfWriter.getInstance(mDocument, new FileOutputStream(mPath));
        mDocument.open();
        return this;
    }

    public PdfImpl addImageToPdf(Bitmap bitmap, int alignment) throws IOException, DocumentException {
        if (null == bitmap) {
            return this;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] datas = baos.toByteArray();

        Image image = Image.getInstance(datas);
        image.setAlignment(alignment);
        mDocument.add(image);
        return this;
    }

    public PdfImpl addImageToPdf(String filename, float width, float height, int alignment) throws IOException, DocumentException {
        if (TextUtils.isEmpty(filename)) {
            return this;
        }
        Image image = Image.getInstance(filename);
        image.setAlignment(alignment);
        image.scaleToFit(width, height);
        mDocument.add(image);
        return this;
    }

    public PdfImpl addTitleToPdf(String content, Font font, int alignment) throws DocumentException {
        return addTextToPdf(content, font, alignment);
    }

    public PdfImpl addTextToPdf(String content, Font font, int alignment) throws DocumentException {
        if (TextUtils.isEmpty(content)) {
            return this;
        }
        Paragraph paragraph = new Paragraph(content, font);
        paragraph.setAlignment(alignment);
        mDocument.add(paragraph);
        return this;
    }

    public void close() {
        if (null == mDocument || !mDocument.isOpen()) {
            return;
        }
        mDocument.close();
        mDocument = null;
    }

    public static class Builder {
        private String mPath;

        private Rectangle mPageSize;
        private float marginLeft;
        private float marginRight;
        private float marginTop;
        private float marginBottom;

        public Builder(String path, Rectangle pageSize) {
            this.mPath = path;
            this.mPageSize = pageSize;
        }

        public Builder setMarginLeft(float marginLeft) {
            this.marginLeft = marginLeft;
            return this;
        }

        public Builder setMarginRight(float marginRight) {
            this.marginRight = marginRight;
            return this;
        }

        public Builder setMarginTop(float marginTop) {
            this.marginTop = marginTop;
            return this;
        }

        public Builder setMarginBottom(float marginBottom) {
            this.marginBottom = marginBottom;
            return this;
        }

        public PdfImpl build() {
            return new PdfImpl(this);
        }

    }

    public interface ParamsKey {
        String FONT = "font";
        String WIDTH = "width";
        String HEIGHT = "height";
        String MARGIN_HORIZONTAL = "margin_horizontal";
        String MARGIN_VERTICAL = "margin_vertical";
    }

}
