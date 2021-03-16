package com.aphrodite.writepaddemo.model.ffmpeg;

import android.text.TextUtils;

public class FFmpegTask {
    static {
        System.loadLibrary("media-handle");
    }

    private final static int RESULT_SUCCESS = 1;

    private final static int RESULT_ERROR = 0;

    //开子线程调用native方法进行音视频处理
    public static void execute(final String[] commands, final ffmpegListener ffmpegListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ffmpegListener != null) {
                    ffmpegListener.onBegin();
                }
                //调用ffmpeg进行处理
                int result = handle(commands);
                if (ffmpegListener != null) {
                    ffmpegListener.onEnd(result, null);
                }
            }
        }).start();
    }

    /**
     * 使用FastStart把Moov移动到Mdat前面
     *
     * @param inputFile  inputFile
     * @param outputFile outputFile
     * @return 是否操作成功
     */
    public int moveMoovAhead(String inputFile, String outputFile) {
        if (TextUtils.isEmpty(inputFile) || TextUtils.isEmpty(outputFile)) {
            return -1;
        }
        return fastStart(inputFile, outputFile);
    }

    /**
     * execute probe cmd internal
     *
     * @param commands       commands
     * @param ffmpegListener onHandleListener
     */
    public static void executeProbe(final String[] commands, final ffmpegListener ffmpegListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ffmpegListener != null) {
                    ffmpegListener.onBegin();
                }
                //execute ffprobe
                String result = handleProbe(commands);
                int resultCode = !TextUtils.isEmpty(result) ? RESULT_SUCCESS : RESULT_ERROR;
                if (ffmpegListener != null) {
                    ffmpegListener.onEnd(resultCode, result);
                }
            }
        }).start();
    }

    private native static int handle(String[] commands);

    private native static int fastStart(String inputFile, String outputFile);

    private native static String handleProbe(String[] commands);

}