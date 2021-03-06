package com.aphrodite.writepaddemo.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by Aphrodite on 2017/9/16.
 */

public class UIUtils {
    /**
     * Screen width
     */
    private static int sDisplayWidthPixels = 0;
    /**
     * Screen height
     */
    private static int sDisplayHeightPixels = 0;

    private static void getDisplayMetrics(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        sDisplayWidthPixels = dm.widthPixels;
        sDisplayHeightPixels = dm.heightPixels;
    }

    public static float getDensity(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.density;
    }

    public static int getDisplayWidthPixels(Context context) {
        if (context == null) {
            return -1;
        }
        if (sDisplayWidthPixels == 0) {
            getDisplayMetrics(context);
        }
        return sDisplayWidthPixels;
    }

    public static int getDisplayHeightPixels(Context context) {
        if (context == null) {
            return -1;
        }
        if (sDisplayHeightPixels == 0) {
            getDisplayMetrics(context);
        }
        return sDisplayHeightPixels;
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static View getRootView(Activity context) {
        return ((ViewGroup) context.findViewById(android.R.id.content)).getChildAt(0);
    }

    public static void openSoftKeyboard(EditText et) {
        InputMethodManager inputManager =
                (InputMethodManager) et.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(et, 0);
    }

    public static void closeSoftKeyboard(Context context) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && ((Activity) context).getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(((Activity) context).getCurrentFocus()
                    .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * ??????????????????
     *
     * @param context Activity ?????????
     * @return int[] ?????????2
     */
    public static int[] getScreenSize(Context context) {
        int[] size = new int[2];
        DisplayMetrics metric = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metric);
        size[0] = metric.widthPixels;
        size[1] = metric.heightPixels;
        return size;
    }

    /**
     * ??????????????????
     *
     * @param activity ?????????
     * @return ?????????
     */
    public static float getScreenBrightness(Activity activity) {
        float value = 0;
        ContentResolver cr = activity.getContentResolver();
        try {
            value = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS);
            return value / 100;
        } catch (Settings.SettingNotFoundException e) {
            return 0.6f;
        }

    }

    /**
     * ??????????????????????????????activity???
     *
     * @param context    ?????????
     * @param brightness ?????????(0~1.0)
     */
    public static void setActivityBrightness(Activity context, float brightness) {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.screenBrightness = brightness;
        context.getWindow().setAttributes(lp);
    }

    /**
     * ??????????????????
     *
     * @param context ?????????
     * @return PowerManager.WakeLock
     */
    public static PowerManager.WakeLock KeepScreenOn(Context context) {
        PowerManager manager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
        PowerManager.WakeLock wakeLock = manager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "aphrodite:light");
        wakeLock.acquire();
        return wakeLock;
    }

    /**
     * ????????????DPI
     *
     * @param activity ?????????
     * @return DIP???
     */
    public static String getPhoneDpi(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        float density = dm.density;
        //ldpi  	0.75 ???
        //mdi		1	???
        //hdpi		1.5	???
        //xhdpi		2	???
        //xxhdpi	3	???
        //xxxhpdi	4	???
        String dpiStr = "mdpi"; //??????mdpi???
        if (0.75 == density)
            dpiStr = "mdpi";
        else if (1 == density)
            dpiStr = "mdpi";
        else if (1.5 == density)
            dpiStr = "hdpi";
        else if (2 == density)
            dpiStr = "xhdpi";
        else if (3 == density)
            dpiStr = "xxhdpi";
        else if (4 == density)
            dpiStr = "xxhdpi";
        return dpiStr;
    }

    /**
     * ?????????????????????
     *
     * @param context ?????????
     * @return ???????????????
     */
    public static int getStatusBarHeight(Context context) {
        if (null == context)
            return 0;

        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
