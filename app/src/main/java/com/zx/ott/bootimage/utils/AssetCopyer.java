package com.zx.ott.bootimage.utils;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: ShaudXiao
 * Date: 2017-06-28
 * Time: 19:25
 * Company: zx
 * Description:
 * FIXME
 */


public class AssetCopyer {

    public static void copyImageUpdate(final AssetManager assetManager, final String assertName) {
        final String srcSd = Constant.DOWNLOAD_TEMP_DIR_PATH;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    copy(assetManager, assertName, srcSd);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if(new File(srcSd, assertName).exists()) {
                    Logger.getLogger().d("**************** shell copy");
                    shellCopy(srcSd + "/" + assertName, "/data/oem/" + assertName);
                    shellCopy(srcSd + "/" + assertName, "/data/oem/" + assertName);
                } else {
                    Logger.getLogger().d("***************assets copy: file not exits!!");
                }

            }
        }).start();
    }


    public static void shellCopy(String src, String dest) {
        final List<String> cmds = new ArrayList<>();
        cmds.add("mv " + src + " " + dest);
        cmds.add("chmod 0777 " + dest);

        ShellUtils.execCmd(cmds, false);
    }

    /**
     *  执行拷贝任务
     *  @param assetName 需要拷贝的assets 名字
     *  @return 拷贝成功后的目标文件句柄
     *  @throws IOException
     */
    public static boolean copy( AssetManager assetManager, String assetName, String dest) throws IOException {
        Logger.getLogger().i("***************assets copy: dest = " + dest);
        InputStream source = assetManager.open(assetName);
        File destinationFile = new File(dest, assetName);
        destinationFile.getParentFile().mkdirs();
        OutputStream destination = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[1024];
        int nread;

        while ((nread = source.read(buffer)) != -1) {
            if (nread == 0) {
                nread = source.read();
                if (nread < 0)
                    break;
                destination.write(nread);
                continue;
            }
            destination.write(buffer, 0, nread);
        }

        destination.close();
        source.close();

        return true;
    }
}
