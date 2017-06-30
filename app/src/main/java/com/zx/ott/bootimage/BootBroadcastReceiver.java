package com.zx.ott.bootimage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zx.ott.bootimage.utils.Logger;

/**
 * User: ShaudXiao
 * Date: 2017-06-29
 * Time: 09:35
 * Company: zx
 * Description:
 * FIXME
 */


public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Logger.getLogger().i("**************** BootBroadcastReceiver************* ");
            wakeUpService(context);
        }
    }

    public static void wakeUpService(Context context) {

//        context.startService(new Intent(context, UpdateBootImageService.class));
    }
}
