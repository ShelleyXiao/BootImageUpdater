package com.zx.ott.bootimage.utils;

/**
 * User: ShaudXiao
 * Date: 2017-06-29
 * Time: 09:59
 * Company: zx
 * Description:
 * FIXME
 */

import android.text.TextUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {

    public HttpUtils() {
    }

    public static InputStream getXML(String path) {
        if(TextUtils.isEmpty(path)) {
            return null;
        }

        try {
            URL url = new URL(path);
            if (url != null) {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("GET");
                int requesetCode = connection.getResponseCode();
                if (requesetCode == 200) {
                    //如果执行成功，返回HTTP响应流
                    return connection.getInputStream();
                }
            }
        } catch (Exception e) {
            Logger.getLogger().e(e.getMessage());

        }

        return null;
    }
}
