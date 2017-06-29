package com.zx.ott.bootimage.utils;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <pre>
 *     author: Blankj
 *     blog  : http://blankj.com
 *     time  : 16/12/08
 *     desc  : Utils初始化相关
 * </pre>
 */
public class Utils {

    private static Context context;

    private Utils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 初始化工具类
     *
     * @param context 上下文
     */
    public static void init(Context context) {
        Utils.context = context.getApplicationContext();
    }

    /**
     * 获取ApplicationContext
     *
     * @return ApplicationContext
     */
    public static Context getContext() {
        if (context != null) return context;
        throw new NullPointerException("u should init first");
    }

    public static boolean compareOtaVersion(String verXmlLocal, String verXmlOnline) {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String date2 = verXmlLocal + " 00:00:00";
        String date3 = verXmlOnline + " 00:00:00";

        Logger.getLogger().d( " date2: " + date2 + " date3: " + date3);

        if (compareDate(date3, date2) > 0)
            return true;

        return false;
    }

    public static int compareDate(String date1, String date2) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            Date dt1 = df.parse(date1);
            Date dt2 = df.parse(date2);

            if (dt1.getTime() > dt2.getTime()) {
                return 1;
            } else if (dt1.getTime() < dt2.getTime()) {
                return -1;
            } else {
                return 0;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return 0;
    }

    public static String getNowTime() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String nowDate = null;
        try {
            Date date = new Date();
            nowDate = df.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nowDate;
    }



}