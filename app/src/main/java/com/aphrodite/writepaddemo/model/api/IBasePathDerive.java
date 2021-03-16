package com.aphrodite.writepaddemo.model.api;

/**
 * Created by Aphrodite on 2021/3/3.
 */
public interface IBasePathDerive {
    /**
     * 获取图片
     */
    void createImageWithPoints(String filename, IPathCallBack callBack);

    /**
     * 获取Video
     */
    void createVideoWithPoints(String filename, int revokeTimes, IPathCallBack callBack);

    /**
     * 获取PDF
     */
    void createPDFWithPoints(String filename, IPathCallBack callBack);

}
