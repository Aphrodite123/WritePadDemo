package com.aphrodite.writepaddemo.model.api;

import java.util.List;
import java.util.Map;

import cn.ugee.mi.optimize.UgeePoint;

/**
 * Created by Aphrodite on 2021/3/3.
 */
public interface IBasePathDerive {
    /**
     * 轨迹转成媒体文件
     *
     * @param ugeePoints 轨迹
     * @param type       类型, image、pdf 或 video
     * @param filename   文件名
     * @param params     配置参数
     * @param callBack   回调
     */
    void createMediaWithPoints(List<UgeePoint> ugeePoints, String type, String filename, Map<String, Object> params, IPathCallBack callBack);

    /**
     * 字符串转PDF文件
     *
     * @param text     文本
     * @param filename 文件名
     * @param params   配置参数
     * @param callBack 回调
     */
    void createPDFWithText(String text, String filename, Map<String, Object> params, IPathCallBack callBack);

    interface Type {
        String IMAGE = "image";
        String PDF = "pdf";
        String VIDEO = "video";
    }

}
