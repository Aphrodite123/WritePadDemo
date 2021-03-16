package com.aphrodite.writepaddemo.model.ffmpeg;

public interface ffmpegListener {
    void onBegin();

    void onEnd(int resultCode, String resultMsg);
}
