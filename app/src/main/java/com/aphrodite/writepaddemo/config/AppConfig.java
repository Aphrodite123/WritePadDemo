package com.aphrodite.writepaddemo.config;

/**
 * Created by Aphrodite on 2019/5/28.
 */
public class AppConfig {
    /**
     * 权限
     */
    public interface PermissionType {
        int BASE = 0x0000;

        int WRITE_EXTERNAL_PERMISSION = BASE + 1;

        int REQUEST_CALL_PERMISSION = BASE + 2;

        int OVERLAY_PERMISSION = BASE + 3;

        int CAMERA_PERMISSION = BASE + 4;

        int LOCATION_PERMISSION = BASE + 5;

        int PHONE_PERMISSION = BASE + 6;

        int CONTACTS_PERMISSION = BASE + 7;
    }

    public interface PenProp {
        //最大横轴
        int MAX_X = 0x6B06;
        //最大纵轴
        int MAX_Y = 0x5014;
        //最大压感
        int MAX_PRESSURE = 0x07FF;
    }

}
