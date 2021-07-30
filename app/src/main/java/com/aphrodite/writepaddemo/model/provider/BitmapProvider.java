package com.aphrodite.writepaddemo.model.provider;

import android.graphics.Bitmap;

import java.util.LinkedList;
import java.util.Queue;

import xyz.mylib.creator.IProviderExpand;

/**
 * Created by Aphrodite on 2021/3/25.
 */
public class BitmapProvider implements IProviderExpand<Bitmap> {
    private Queue<Bitmap> mQueue;

    public BitmapProvider() {
        this.mQueue = new LinkedList<>();
    }

    @Override
    public synchronized boolean hasNext() {
        return null == mQueue || mQueue.size() <= 0 ? false : true;
    }

    @Override
    public synchronized int size() {
        return null == mQueue ? 0 : mQueue.size();
    }

    @Override
    public synchronized Bitmap next() {
        if (null == mQueue || mQueue.size() <= 0) {
            return null;
        }
        return mQueue.poll();
    }

    @Override
    public void prepare() {
    }

    @Override
    public synchronized void finish() {
        if (null != mQueue) {
            mQueue.clear();
        }
    }

    @Override
    public synchronized void finishItem(Bitmap item) {
        if (null != item) {
            item.recycle();
            item = null;
        }
    }

    public synchronized void setQueue(Bitmap bitmap) {
        if (null == bitmap) {
            return;
        }
        mQueue.add(bitmap);
    }
}
