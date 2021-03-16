package com.aphrodite.writepaddemo.model.api;

/**
 * Created by Aphrodite on 2021/3/15.
 */
public interface IPathCallBack {
    void success(String path);

    void failed(int code);
}
