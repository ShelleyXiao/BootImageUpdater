package com.zx.ott.bootimage;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.zx.ott.bootimage.downloader.downmanger.bizs.DLManager;
import com.zx.ott.bootimage.downloader.downmanger.interfaces.SimpleDListener;
import com.zx.ott.bootimage.utils.AppCacheUtils;
import com.zx.ott.bootimage.utils.AssetCopyer;
import com.zx.ott.bootimage.utils.Constant;
import com.zx.ott.bootimage.utils.FileUtils;
import com.zx.ott.bootimage.utils.HttpUtils;
import com.zx.ott.bootimage.utils.Logger;
import com.zx.ott.bootimage.utils.NetWorkUtil;
import com.zx.ott.bootimage.utils.SharePrefUtil;
import com.zx.ott.bootimage.utils.ShellUtils;
import com.zx.ott.bootimage.utils.Utils;
import com.zx.ott.bootimage.utils.XmlUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * User: ShaudXiao
 * Date: 2017-06-27
 * Time: 19:37
 * Company: zx
 * Description:
 * FIXME
 */


public class UpdateBootImageService extends Service {

    private static final int FISISH_DOWNLOAD_XML = 1;
    private static final int FISISH_DOWNLOAD_ZIP = 2;
    private static final int FISISH_DOWNLOAD_MP4 = 4;
    private static final int FISISH_DOWNLOAD_JPG = 5;

    private static final int ERROR_DOWNLOAD_XML = 6;
    private static final int ERROR_DOWNLOAD_ZIP = 7;
    private static final int ERROR_DOWNLOAD_MP4 = 8;
    private static final int ERROR_DOWNLOAD_JPG = 9;

    private static final int START_DOWNLOAD = 10;

    private static final String BOOTLOGO_DOWNLOAD_TMEP_PATH = Constant.DOWNLOAD_TEMP_DIR_PATH + "/" + Constant.BOOTANIMATION_JPG;
    private static final String BOOTANIMATION_DOWNLOAD_TMEP_PATH = Constant.DOWNLOAD_TEMP_DIR_PATH + "/" + Constant.BOOTANIMATION_ZIP;
    private static final String BOOTVIDEO_DOWNLOAD_TMEP_PATH = Constant.DOWNLOAD_TEMP_DIR_PATH + "/" + Constant.BOOTANIMATION_MP4;


    private DownloadManager mDownloadManager;
    private DownloadManager.Request mRequest;

//    private NetWorkStatusReceiver mNetworkChangeReceiver;
//    private NetWorkStatusReceiver mNetworkChangeReceiver1;

    private MyHandler mMyHandler;

    private String mXmlFileUri = Constant.BASE_URL + Constant.UPDATE_XML;
    private String mXmlFileName = Constant.UPDATE_XML;

    private boolean checkedAnimation;
    private boolean checkedBootlogo;
    private boolean checkBootVideo;

    private boolean needUpdateAnimation;
    private boolean needUpdateBootimage;
    private boolean needUpdateBootVideo;


    public static enum DownloadFileType {
        JPG,
        MP4,
        ZIP,
        XML
    };

    private HashMap<DownloadFileType, UpdateInfo> mUpdateInfoMap = new HashMap<>();


    @Override
    public void onCreate() {
        super.onCreate();
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        mMyHandler = new MyHandler();

        AssetCopyer.copyImageUpdate(getAssets(), "image_updater");    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.getLogger().i("onStartCommand******************");

        checkedAnimation = intent.getBooleanExtra(Constant.KEY_FIRST_UPDATE_ZIP, false);
        checkBootVideo = intent.getBooleanExtra(Constant.KEY_FIRST_UPDATE_VIDEO, false);
        checkedBootlogo = intent.getBooleanExtra(Constant.KEY_FIRST_UPDATE_JPG, false);

        Logger.getLogger().d("checkedAnimation =  " + checkedAnimation + " checkBootVideo = " + checkBootVideo
                + " checkedBootlogo = " + checkedBootlogo);

        checkUpdateInfo();


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.getLogger().i("onDestroy******************");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkUpdateInfo() {

        if (NetWorkUtil.isNetWorkAvailable(this)) {
            new GetXmlTask().execute(mXmlFileUri);
        } else {
            Logger.getLogger().d("NetWork disconnected!!");
        }
    }


    private void compareUpdateInfo() {
        if (mUpdateInfoMap.size() <= 0) {
            mMyHandler.sendEmptyMessage(ERROR_DOWNLOAD_XML);
            return;
        }

        String lastUpdateJpg = SharePrefUtil.getString(this, Constant.KEY_LAST_UPDATE_JPG_TIME, Constant.DEFALUT_INIT_UPDATE_TIME);
        String lastUpdateZip = SharePrefUtil.getString(this, Constant.KEY_LAST_UPDATE_ZIP_TIME, Constant.DEFALUT_INIT_UPDATE_TIME);
        String lastUpdateVideo = SharePrefUtil.getString(this, Constant.KEY_LAST_UPDATE_VIDEO_TIME, Constant.DEFALUT_INIT_UPDATE_TIME);
        for (Map.Entry<DownloadFileType, UpdateInfo> enty : mUpdateInfoMap.entrySet()) {
            UpdateInfo info = enty.getValue();
            if (info.getName().equals(Constant.BOOTANIMATION_JPG)) {
                if (Utils.compareOtaVersion(lastUpdateJpg, info.getVersion())
                        && checkedBootlogo) {
                    needUpdateBootimage = true;
                    Logger.getLogger().i("************** needUpdateBootimage");
                }
            } else if (info.getName().equals(Constant.BOOTANIMATION_ZIP)
                    && checkedAnimation) {
                if (Utils.compareOtaVersion(lastUpdateZip, info.getVersion())) {
                    needUpdateAnimation = true;
                    Logger.getLogger().i("************** needUpdateAnimation");
                }
            } else if (info.getName().equals(Constant.BOOTANIMATION_MP4)) {
                if (Utils.compareOtaVersion(lastUpdateVideo, info.getVersion())
                        && checkBootVideo) {
                    needUpdateBootVideo = true;
                    Logger.getLogger().i("************** needUpdateBootVideo");
                }
            }
        }

        if (needUpdateBootimage || needUpdateBootVideo || needUpdateAnimation) {
            mMyHandler.sendEmptyMessage(START_DOWNLOAD);
        }
    }

    private void showToast(String arg) {
        Toast.makeText(this, arg, Toast.LENGTH_SHORT).show();
    }

    private void startDownloadAnimation() {
        UpdateInfo annimation = mUpdateInfoMap.get(DownloadFileType.ZIP);
        if (null != annimation) {
            Logger.getLogger().d("startDownloadAnimation *********** url " + annimation.getDownloadUrl());
            DLManager.getInstance(this).dlStart(annimation.getDownloadUrl(),
                    AppCacheUtils.getExternalCacheDirectory(this, Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                    new SimpleDListener() {
                        @Override
                        public void onPrepare() {
                            Logger.getLogger().d("******startDownloadAnimation  onPrepare");
                        }

                        @Override
                        public void onProgress(int progress) {
                            Logger.getLogger().d("******startDownloadAnimation  progress: " + progress);
                        }

                        @Override
                        public void onFinish(File file) {
                            Logger.getLogger().d("******startDownloadAnimation  onFinish file" + file.getAbsolutePath());
                            mMyHandler.sendEmptyMessage(FISISH_DOWNLOAD_ZIP);
                        }

                        @Override
                        public void onError(int status, String error) {
                            Logger.getLogger().d("******startDownloadAnimation  error: " + error);
                            mMyHandler.sendEmptyMessage(ERROR_DOWNLOAD_ZIP);

                        }
                    });
        }

    }

    private void startDownloadBootlogo() {
        UpdateInfo bootlogo = mUpdateInfoMap.get(DownloadFileType.JPG);
        if (null != bootlogo) {
            Logger.getLogger().d("startDownloadAnimation *********** url " + bootlogo.getDownloadUrl());
            DLManager.getInstance(this).dlStart(bootlogo.getDownloadUrl(),
                    AppCacheUtils.getExternalCacheDirectory(this, Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                    new SimpleDListener() {
                        @Override
                        public void onPrepare() {
                            Logger.getLogger().d("******startDownloadBootlogo  onPrepare");
                        }

                        @Override
                        public void onProgress(int progress) {
                            Logger.getLogger().d("******startDownloadBootlogo  progress: " + progress);
                        }

                        @Override
                        public void onFinish(File file) {
                            Logger.getLogger().d("******startDownloadBootlogo  onFinish file" + file.getAbsolutePath());
                            mMyHandler.sendEmptyMessage(FISISH_DOWNLOAD_JPG);
                        }

                        @Override
                        public void onError(int status, String error) {
                            Logger.getLogger().d("******startDownloadBootlogo  error: " + error);
                            mMyHandler.sendEmptyMessage(ERROR_DOWNLOAD_JPG);
                        }
                    });
        }

    }

    private void startDownloadBootVideo() {
        UpdateInfo bootVideo = mUpdateInfoMap.get(DownloadFileType.MP4);
        if (null != bootVideo) {
            Logger.getLogger().d("startDownloadBootVideo *********** url " + bootVideo.getDownloadUrl());
            DLManager.getInstance(this).dlStart(bootVideo.getDownloadUrl(),
                    AppCacheUtils.getExternalCacheDirectory(this, Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                    new SimpleDListener() {
                        @Override
                        public void onPrepare() {
                            Logger.getLogger().d("******startDownloadBootVideo  onPrepare");
                        }

                        @Override
                        public void onProgress(int progress) {
                            Logger.getLogger().d("******startDownloadBootVideo  progress: " + progress);
                        }

                        @Override
                        public void onFinish(File file) {
                            Logger.getLogger().d("******startDownloadBootVideo  onFinish file" + file.getAbsolutePath());
                            mMyHandler.sendEmptyMessage(FISISH_DOWNLOAD_MP4);
                        }

                        @Override
                        public void onError(int status, String error) {
                            Logger.getLogger().d("******startDownloadBootVideo  error: " + error);
                            mMyHandler.sendEmptyMessage(ERROR_DOWNLOAD_MP4);
                        }
                    });
        }

    }

    private void copyFileToData(String fileName) {
        final List<String> cmds = new ArrayList<>();
        cmds.add("mv -f "
                + AppCacheUtils.getExternalCacheDirectory(this, Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + "/" + fileName
                + "  /data/oem");
        cmds.add("chmod 0644 /data/oem/" + fileName);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ShellUtils.execCmd(cmds, false);
            }
        }).start();
    }

    private void copyBootanimtion() {
        final List<String> cmds = new ArrayList<>();
        cmds.add("mv -f "
                + AppCacheUtils.getExternalCacheDirectory(this, Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + "/" + Constant.BOOTANIMATION_ZIP
                + "  /data/oem");
        cmds.add("chmod 0644 /data/oem/" + Constant.BOOTANIMATION_ZIP);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ShellUtils.execCmd(cmds, false);
            }
        }).start();
    }

    private void copyBootlogo() {
        final List<String> cmds = new ArrayList<>();
        cmds.add("mv -f "
                + AppCacheUtils.getExternalCacheDirectory(this, Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + "/" + Constant.BOOTANIMATION_JPG
                + "  /data/oem");
        cmds.add("chmod 0644 /data/oem/" + Constant.BOOTANIMATION_JPG);

        new Thread(new Runnable() {

            @Override
            public void run() {
                ShellUtils.execCmd(cmds, false);
            }
        }).start();
    }

    private void copyBootVideo() {
        final List<String> cmds = new ArrayList<>();
        cmds.add("mv -f "
                + AppCacheUtils.getExternalCacheDirectory(this, Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + "/" + Constant.BOOTANIMATION_MP4
                + "  /data/oem/vendor_video.mp4");
        cmds.add("chmod 0644 /data/oem/vendor_video.mp4");

        new Thread(new Runnable() {

            @Override
            public void run() {
                ShellUtils.execCmd(cmds, false);
            }
        }).start();
    }

    private class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FISISH_DOWNLOAD_XML:
                    compareUpdateInfo();
                    break;

                case ERROR_DOWNLOAD_XML:
                    showToast(getString(R.string.get_update_xml_failed));
                    break;
                case START_DOWNLOAD:
                    if (needUpdateAnimation) {
                        startDownloadAnimation();
                    }
                    if (needUpdateBootVideo) {
                        startDownloadBootVideo();
                    }
                    if (needUpdateBootimage) {
                        startDownloadBootlogo();
                    }
                    break;
                case FISISH_DOWNLOAD_JPG:
                    copyFileToData(Constant.BOOTANIMATION_JPG);;
                    SharePrefUtil.saveString(UpdateBootImageService.this, Constant.KEY_LAST_UPDATE_JPG_TIME, Utils.getNowTime());
                    showToast("download bootlogo finished ");
                    break;
                case ERROR_DOWNLOAD_JPG:
                    showToast("download bootlogo faild");
                    FileUtils.deleteFile(BOOTLOGO_DOWNLOAD_TMEP_PATH);
                    break;
                case FISISH_DOWNLOAD_ZIP:
                    copyFileToData(Constant.BOOTANIMATION_ZIP);
                    showToast("download veder_video finished");
                    SharePrefUtil.saveString(UpdateBootImageService.this, Constant.KEY_LAST_UPDATE_ZIP_TIME, Utils.getNowTime());
                    break;
                case ERROR_DOWNLOAD_ZIP:
                    showToast("download bootanimation faild");
                    FileUtils.deleteFile(BOOTANIMATION_DOWNLOAD_TMEP_PATH);
                    break;
                case FISISH_DOWNLOAD_MP4:
                    copyBootVideo();
                    SharePrefUtil.saveString(UpdateBootImageService.this, Constant.KEY_LAST_UPDATE_VIDEO_TIME, Utils.getNowTime());
                    showToast("download veder_video finished");
                    break;
                case ERROR_DOWNLOAD_MP4:
                    showToast("download veder_video faild");
                    FileUtils.deleteFile(BOOTVIDEO_DOWNLOAD_TMEP_PATH);
                    break;
            }

            super.handleMessage(msg);

        }
    }

    private class DownLoadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {

            } else if (intent.getAction().equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
                Toast.makeText(UpdateBootImageService.this, "Does not support yet", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class GetXmlTask extends AsyncTask<String, Void, HashMap<DownloadFileType, UpdateInfo>> {


        public GetXmlTask() {
        }

        @Override
        protected HashMap<DownloadFileType, UpdateInfo> doInBackground(String... strings) {
            String urlPath = strings[0];
            HashMap<DownloadFileType, UpdateInfo> infos = null;
            Logger.getLogger().i("******doInBackground******* ");
//            try {
//                URL url = new URL(urlPath);
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setConnectTimeout(5000);
//                conn.setRequestMethod("GET");
//                Log.i("CONN", conn.toString() + " code = " + conn.getResponseCode());
//
//                if (conn.getResponseCode() == 200) {
//                    InputStream ins = conn.getInputStream();
//                    return (HashMap) XmlUtils.loadOtaPackageInfo(ins);
//                }
//
//                InputStream ins = conn.getInputStream();
//                return (HashMap) XmlUtils.loadOtaPackageInfo(ins);
//            } catch (Exception e) {
////                e.printStackTrace();
//                e.printStackTrace();
//                return infos;
//
//            }

            InputStream ins = HttpUtils.getXML(urlPath);
            return (HashMap) XmlUtils.loadOtaPackageInfo(ins);

        }

        @Override
        protected void onPostExecute(HashMap<DownloadFileType, UpdateInfo> maps) {
            Logger.getLogger().i("*****onPostExecute******* ");
            if (maps != null && maps.size() > 0) {
                mUpdateInfoMap.putAll(maps);
                mMyHandler.sendEmptyMessage(FISISH_DOWNLOAD_XML);
            } else {
                mMyHandler.sendEmptyMessage(ERROR_DOWNLOAD_XML);
            }
        }
    }
}
