package com.aphrodite.writepaddemo.model.provider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.aphrodite.writepaddemo.utils.BitmapUtils;
import com.aphrodite.writepaddemo.utils.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import xyz.mylib.creator.IProviderExpand;

/**
 * Created by Aphrodite on 2021/3/25.
 */
public class BitmapProvider implements IProviderExpand<Bitmap> {
    private Queue<byte[]> mQueue;
    private int[] mScaleSize;
    private int mIndex = 0;
    private int mCount;

    public BitmapProvider(String path, int[] scaleSize) {
        this.mQueue = new LinkedList<>();
        this.mScaleSize = scaleSize;
        initData(path);
    }

    private void initData(String path) {
        File[] files = FileUtils.listFiles(path);
        if (null == files || files.length <= 0) {
            return;
        }
        List<File> fileList = Arrays.asList(files);
        mCount = fileList.size();
        //按照文件名称升序排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (null == o1 || null == o2) {
                    return 0;
                }
                String name1 = o1.getName();
                String name2 = o2.getName();
                int num1 = Integer.valueOf(name1.substring(0, name1.lastIndexOf(".")));
                int num2 = Integer.valueOf(name2.substring(0, name2.lastIndexOf(".")));
                return num1 - num2;
            }
        });
        for (File file : fileList) {
            if (null == file) {
                continue;
            }
            Log.i("initData","Start bitmap.");
            Bitmap bitmap = BitmapUtils.decodeSampleBitmapFromResource(file.getAbsolutePath(), mScaleSize[0], mScaleSize[1]);
            Log.i("initData","End bitmap.");
            mQueue.add(BitmapUtils.bitmapToBytes(bitmap));
            bitmap.recycle();
        }

    }

    @Override
    public boolean hasNext() {
        if (null == mQueue || mQueue.size() <= 0) {
            return false;
        }
        return mIndex < mQueue.size();
    }

    @Override
    public int size() {
        return mCount;
    }

    @Override
    public Bitmap next() {
        if (null == mQueue || mQueue.size() <= 0) {
            return null;
        }
        byte[] bytes = mQueue.poll();
        mIndex++;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    public void prepare() {

    }

    @Override
    public void finish() {

    }

    @Override
    public void finishItem(Bitmap item) {
        if (null != item) {
            item.recycle();
            item = null;
        }
    }

}
