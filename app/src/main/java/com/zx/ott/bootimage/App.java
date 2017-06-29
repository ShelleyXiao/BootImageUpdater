package com.zx.ott.bootimage;

import android.app.Application;

import com.zx.ott.bootimage.utils.Utils;

/**
 * User: ShaudXiao
 * Date: 2017-06-27
 * Time: 15:50
 * Company: zx
 * Description:
 * FIXME
 */


public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(this);
    }
}
