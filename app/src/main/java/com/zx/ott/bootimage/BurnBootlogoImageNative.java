package com.zx.ott.bootimage;

/**
 * User: ShaudXiao
 * Date: 2017-06-29
 * Time: 14:30
 * Company: zx
 * Description:
 * FIXME
 */


public class BurnBootlogoImageNative {

    static {
        System.loadLibrary("burnbootlogo");
    }

    static native int wrtieRawImage(String filePath, String partName);
}
