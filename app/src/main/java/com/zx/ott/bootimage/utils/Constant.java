package com.zx.ott.bootimage.utils;

/**
 * User: ShaudXiao
 * Date: 2017-06-27
 * Time: 16:16
 * Company: zx
 * Description:
 * FIXME
 */


public class Constant {

    public static final String BASE_URL = "http://203.110.167.99/download/shandong/lutong_test/";

    public static final String BOOTANIMATION_ZIP = "bootanimation.zip";
    public static final String BOOTANIMATION_MP4 = "vendor_video.mpeg";
    public static final String BOOTANIMATION_JPG = "splash.zip"; //"logo.jpg";

    public static final String UPDATE_XML = "update.xml";

    public static final String KEY_FIRST_UPDATE_JPG = "fisrt_update_jpg";
    public static final String KEY_FIRST_UPDATE_ZIP = "fisrt_update_zip";
    public static final String KEY_FIRST_UPDATE_VIDEO = "fisrt_update_video";

    public static final String DEFALUT_INIT_UPDATE_TIME = "2017-06-26";

    public static final String KEY_LAST_UPDATE_JPG_TIME = "last_update_jpg_time";
    public static final String KEY_LAST_UPDATE_ZIP_TIME = "last_update_zip_time";
    public static final String KEY_LAST_UPDATE_VIDEO_TIME = "last_update_video_time";

    public static final String BOOTANIMATION_DEST_PATH = " /system/media";
    public static final String BOOTVIDEO_DEST_PATH = "";

    public static final String REMOUNT_SYSTEM_PATTION = "mount -o remount,rw /system";

    public static final String DOWNLOAD_TEMP_DIR_PATH = "/mnt/sdcard/Android/data/com.zx.ott.bootimage/files/Download";
    public static final String COPY_TEMP_DIR_PATH = "/mnt/sdcard/Download/";

    public static final String BURN_BOOTLOGO_EXE = "image_updater";
}
