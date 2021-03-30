package com.aphrodite.writepaddemo.model.api;

import java.util.Map;

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
     * 图片转PDF
     */
    void createPDFWithPoints(String filename, IPathCallBack callBack);

    /**
     * 字符串转PDF
     */
    void createPDFWithText(String text, String filename, Map<String, Object> params, IPathCallBack callBack);

}
