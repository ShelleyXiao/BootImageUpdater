package com.zx.ott.bootimage;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.zx.ott.bootimage.utils.Constant;
import com.zx.ott.bootimage.utils.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class SystemUpdate extends PreferenceActivity implements
        OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = "SystemUpdateActivity";
    private static final String FILENAME_PROC_VERSION = "/proc/version";

    private static final String mXmlFileUri = Constant.BASE_URL + "";

    private static final String KEY_UPDATE_BOOTLOGO = "update_bootlogo";
    private static final String KEY_UPDATE_VENDOR_VIDEO = "update_vendor_video";
    private static final String KEY_UPDATE_ANIMATION = "update_animation";

    public static final String CONNECTIVITY_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    private static final int MSG_SHOW_INSTALL_PROGRESS = 0;
    private static final int MSG_VERIFY_OTA_PACKAGE_SHOW = 1;
    private static final int MSG_VERIFY_OTA_PACKAGE_PROGRESS = 2;
    private static final int MSG_VERIFY_OTA_PACKAGE_CLOSE = 3;

    protected static final int DOWNLOAD_TO_SYSTEM_CACHE = 1;
    protected static final int DOWNLOAD_TO_DOWNLOAD_CACHE_DIR = 2;

    private enum State {NONE, CHECK_INFO_XML, DOWNLOAD_PACKAGE, INSTALL_PACKAGE}

    ;
    private State mState = State.NONE;

    private static final HandlerThread sWorkerThread = new HandlerThread("network-loader");

    static {
        sWorkerThread.start();
    }

    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    private Handler mHandler = new Handler();

    private String mFileType = "text/plain";

    private MyHandler mMyHandler;

    private boolean mIsNetWorkConnected;

    private CheckBoxPreference mUpdatebootanimation;
    private CheckBoxPreference mUpdatebootlogo;
    private CheckBoxPreference mUpdatebootVideo;

    private boolean checkedAnimation;
    private boolean checkedBootlogo;
    private boolean checkBootVideo;

    private ProgressBar mProgressBar;
    private Button mCheckButton;


    private AlertDialog mDialog;
    private ProgressDialog mProcessDialog;
    private ProgressDialog mProcessVerifyOta;

    private boolean mIsCheckingCanceled = false;

    private LayoutInflater mInflater;

    private DownloadManager mDownloadManager;
    private Request mRequest;

    private NetWorkStatusReceiver mNetworkChangeReceiver;
    private NetWorkStatusReceiver mNetworkChangeReceiver1;

    private SharedPreferences mSharedPrefs;


    // Just a few popular file types used to return from a download
    protected enum DownloadFileType {
        PLAINTEXT,
        APK,
        GIF,
        GARBAGE,
        UNRECOGNIZED,
        ZIP,
        XML
    }

    protected enum DataType {
        TEXT,
        BINARY
    }

    private void runOnMainThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            mHandler.post(r);
        } else {
            r.run();
        }
    }

    private void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }


    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SHOW_INSTALL_PROGRESS:
                    int status = (Integer) msg.obj;

                    if (isDownloading(status)) {
                        mProgressBar.setVisibility(View.VISIBLE);
                        mProgressBar.setMax(0);
                        mProgressBar.setProgress(0);
                        mCheckButton.setEnabled(false);

                        if (msg.arg2 < 0) {
                            mProgressBar.setIndeterminate(true);
                        } else {
                            mProgressBar.setIndeterminate(false);
                            mProgressBar.setMax(msg.arg2);
                            mProgressBar.setProgress(msg.arg1);
                        }
                    } else {
                        mProgressBar.setVisibility(View.GONE);
                        mProgressBar.setMax(0);
                        mProgressBar.setProgress(0);
                        mCheckButton.setEnabled(true);

                        if (status == DownloadManager.STATUS_FAILED) {
//                        mDownloadSize.setText(getString(R.string.status_download_failed));
                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
//                        mDownloadSize.setText(getString(R.string.status_download_ok));
//                        mDownloadPercent.setText("");
                        }
                    }
                    break;
                case MSG_VERIFY_OTA_PACKAGE_SHOW:
                    break;
                case MSG_VERIFY_OTA_PACKAGE_PROGRESS:
                    break;
                case MSG_VERIFY_OTA_PACKAGE_CLOSE:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.system_update);
        setContentView(R.layout.system_update_layout);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mMyHandler = new MyHandler();

        mCheckButton = (Button) findViewById(R.id.check_update);
        mCheckButton.setText("Check Now");
        mCheckButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(LOG_TAG, "button clicked");
                checkUpdate();
            }
        });

        mUpdatebootanimation = (CheckBoxPreference) findPreference(KEY_UPDATE_ANIMATION);
        mUpdatebootlogo = (CheckBoxPreference) findPreference(KEY_UPDATE_BOOTLOGO);
        mUpdatebootVideo = (CheckBoxPreference) findPreference(KEY_UPDATE_VENDOR_VIDEO);

        mUpdatebootanimation.setOnPreferenceClickListener(this);
        mUpdatebootanimation.setOnPreferenceChangeListener(this);
        mUpdatebootlogo.setOnPreferenceClickListener(this);
        mUpdatebootlogo.setOnPreferenceChangeListener(this);
        mUpdatebootVideo.setOnPreferenceClickListener(this);
        mUpdatebootVideo.setOnPreferenceChangeListener(this);


        NetWorkStatusReceiver mNetworkChangeReceiver = new NetWorkStatusReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CONNECTIVITY_CHANGE_ACTION);
        intentFilter.setPriority(1000);
        registerReceiver(mNetworkChangeReceiver, intentFilter);
        mNetworkChangeReceiver1 = mNetworkChangeReceiver;

        updateCheckValue();
        /* checkUpdate(); */
    }


    private void updateCheckValue() {
        checkedAnimation = mUpdatebootanimation.isChecked();
        checkBootVideo = mUpdatebootVideo.isChecked();
        checkedBootlogo = mUpdatebootlogo.isChecked();

        Logger.getLogger().d("checkedAnimation =  " + checkedAnimation + " checkBootVideo = " + checkBootVideo
                        + " checkedBootlogo = " + checkedBootlogo);
    }

    private final DialogInterface.OnDismissListener mOnDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            mIsCheckingCanceled = true;
            mProcessDialog = null;
        }
    };

    private void showCheckingDialog(Context context, String message, boolean isCancelable) {
        if (mProcessDialog == null) {
            mProcessDialog = new ProgressDialog(context, R.style.dialog);
            mProcessDialog.setCanceledOnTouchOutside(false);
            mProcessDialog.setCancelable(isCancelable);
            mProcessDialog.setIndeterminate(true);
            mProcessDialog.setOnDismissListener(mOnDismissListener);
            mProcessDialog.setMessage(message);
            mProcessDialog.show();
        }
    }

    private void closeCheckingDialog() {
        if (mProcessDialog != null) {
            if (mProcessDialog.isShowing()) {
                mProcessDialog.cancel();
            }
            mProcessDialog = null;
        }
    }

    private void checkUpdate() {

        if (!isNetworkConnected(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Unable to connect");
            builder.setMessage("You need a network connection to use this app. " +
                    "Please turn on Mobile Data or WLAN in Settings");
            builder.setPositiveButton("SETTINGS", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        } else {
            checkUpdateInfo();
        }
    }

    private void checkUpdateInfo() {
//        showCheckingDialog(this, "CHECKING...", true);

        Intent intent = new Intent(this, UpdateBootImageService.class);
        intent.putExtra(Constant.KEY_FIRST_UPDATE_JPG, checkedBootlogo);
        intent.putExtra(Constant.KEY_FIRST_UPDATE_ZIP, checkedAnimation);
        intent.putExtra(Constant.KEY_FIRST_UPDATE_VIDEO, checkBootVideo);

        startService(intent);
    }


    private void handlePreferenceClick(Preference preference) {

        Log.i(LOG_TAG, "onPreferenceChange------>" + String.valueOf(preference.getKey()));
        if (preference.getKey().equals(KEY_UPDATE_ANIMATION)) {
            Log.e(LOG_TAG, "mUpdatebootanimationis checked: handlePreferenceClick");
            Boolean prefsValue = mSharedPrefs.getBoolean(preference.getKey(), false);
            showToast(prefsValue.toString());

        } else if (preference.getKey().equals(KEY_UPDATE_BOOTLOGO)) {
            Log.e(LOG_TAG, "KEY_UPDATE_BOOTLOGO checked: handlePreferenceClick");

        } else if (preference.getKey().equals(KEY_UPDATE_VENDOR_VIDEO)) {
            Log.e(LOG_TAG, "KEY_UPDATE_VENDOR_VIDEO is checked: handlePreferenceClick");

        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
//        Log.i(LOG_TAG, "onPreferenceClick------>" + String.valueOf(preference.getKey()));
//        handlePreferenceClick(preference);

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference.getKey().equals(KEY_UPDATE_ANIMATION)) {
            Log.e(LOG_TAG, "mUpdatebootanimationis checked: " + String.valueOf(objValue));
            String prefsValue = objValue.toString();
            showToast(prefsValue.toString());
            mUpdatebootanimation.setChecked(Boolean.valueOf(prefsValue));
        } else if (preference.getKey().equals(KEY_UPDATE_BOOTLOGO)) {
            Log.e(LOG_TAG, "KEY_UPDATE_BOOTLOGO checked: " + String.valueOf(objValue));
            String prefsValue = objValue.toString();
            showToast(prefsValue);
            mUpdatebootlogo.setChecked(Boolean.valueOf(prefsValue));
        } else if (preference.getKey().equals(KEY_UPDATE_VENDOR_VIDEO)) {
            Log.e(LOG_TAG, "KEY_UPDATE_VENDOR_VIDEO is checked: " + String.valueOf(objValue));
            String prefsValue = objValue.toString();
            showToast(prefsValue);
            mUpdatebootVideo.setChecked(Boolean.valueOf(prefsValue));
        }

        updateCheckValue();

        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        handlePreferenceClick(preference);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        return super.onKeyDown(keyCode, event);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (null != mNetworkChangeReceiver1)
            unregisterReceiver(mNetworkChangeReceiver1);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        //mDownloadManager.restartDownload(mDownloadId);
    }

    private boolean isDownloading(int downloadStatus) {
        return downloadStatus == DownloadManager.STATUS_RUNNING ||
                downloadStatus == DownloadManager.STATUS_PAUSED ||
                downloadStatus == DownloadManager.STATUS_PENDING;
    }

    private static DecimalFormat DOUBLE_DECIMAL_FORMAT = new DecimalFormat("0.##");
    private static final int MB_2_BYTE = 1024 * 1024;
    private static final int KB_2_BYTE = 1024;


    public static String getNotiPercent(long progress, long max) {
        int rate = 0;
        if (progress <= 0 || max <= 0) {
            rate = 0;
        } else if (progress > max) {
            rate = 100;
        } else {
            rate = (int) ((double) progress / max * 100);
        }
        return new StringBuilder(16).append(rate).append("%").toString();
    }


    public int getDownloadStatus(long downloadId) {
        int[] bytes = getSizesAndStatus(downloadId);

        return bytes[2];
    }

    public int[] getDownloadSizes(long downloadId) {
        int[] bytes = getSizesAndStatus(downloadId);
        return new int[]{bytes[0], bytes[1]};
    }

    public int[] getSizesAndStatus(long downloadId) {
        int[] bytes = new int[]{-1, -1, 0};
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = null;
        try {
            c = mDownloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                bytes[0] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                bytes[1] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                bytes[2] = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return bytes;
    }

    private int getFailReason(long downloadId) {
        int reason = DownloadManager.ERROR_UNKNOWN;
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = null;
        try {
            c = mDownloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                int columnReason = c.getColumnIndex(DownloadManager.COLUMN_REASON);
                reason = c.getInt(columnReason);
                switch (reason) {
                    case DownloadManager.ERROR_CANNOT_RESUME:
                        Log.e(LOG_TAG, "some possibly transient error occurred but we can't resume the download");
                        break;
                    case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                        Log.e(LOG_TAG, "no external storage device was found. Typically, this is because the SD card is not mounted");
                        break;
                    case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                        Log.e(LOG_TAG, "the requested destination file already exists (the download manager will not overwrite an existing file)");
                        break;
                    case DownloadManager.ERROR_FILE_ERROR:
                        Log.e(LOG_TAG, "a storage issue arises which doesn't fit under any other error code");
                        break;
                    case DownloadManager.ERROR_HTTP_DATA_ERROR:
                        Log.e(LOG_TAG, "an error receiving or processing data occurred at the HTTP level");
                        break;
                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                        Log.e(LOG_TAG, "here was insufficient storage space. Typically, this is because the SD card is full");
                        break;
                    case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                        Log.e(LOG_TAG, "there were too many redirects");
                        break;
                    case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                        Log.e(LOG_TAG, "an HTTP code was received that download manager can't handle");
                        break;
                    case DownloadManager.ERROR_UNKNOWN:
                        Log.e(LOG_TAG, "he download has completed with an error that doesn't fit under any other error code");
                        break;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return reason;
    }

    public String getDownloadFilePath(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        String path = null, fileName = null;
        Cursor c = null;
        try {
            c = mDownloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                path = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                fileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return new StringBuilder(path).append(File.separator).append(fileName).toString();
    }

    public static boolean installAPK(Context context, String filePath) {
        Intent i = new Intent(Intent.ACTION_VIEW);

        File file = new File(filePath);
        if (file != null && file.length() > 0 && file.exists() && file.isFile()) {
            i.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            return true;
        }
        return false;
    }


    /**
     * Sets the MIME type of file that will be served from the mock server
     *
     * @param type The MIME type to return from the server
     */
    protected void setServerMimeType(DownloadFileType type) {
        mFileType = getMimeMapping(type);
    }

    /**
     * Gets the MIME content string for a given type
     *
     * @param type The MIME type to return
     * @return the String representation of that MIME content type
     */
    protected String getMimeMapping(DownloadFileType type) {
        switch (type) {
            case APK:
                return "application/vnd.android.package-archive";
            case GIF:
                return "image/gif";
            case ZIP:
                return "application/x-zip-compressed";
            case GARBAGE:
                return "zip\\pidy/doo/da";
            case UNRECOGNIZED:
                return "application/new.undefined.type.of.app";
            case XML:
                return "text/xml";
        }
        return "text/plain";
    }


    private void showToast(String arg) {
        Toast.makeText(this, arg, Toast.LENGTH_SHORT).show();
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

    public static String utc2Local(String utcTime, String utcTimePatten, String localTimePatten) {
        SimpleDateFormat utcFormater = new SimpleDateFormat(utcTimePatten);
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date gpsUTCDate = null;
        try {
            gpsUTCDate = utcFormater.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat localFormater = new SimpleDateFormat(localTimePatten);
        localFormater.setTimeZone(TimeZone.getDefault());
        String localTime = localFormater.format(gpsUTCDate.getTime());
        return localTime;
    }


    /**
     * Reads a line from the specified file.
     *
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getSystemCachePath(Context context) {
        return Environment.getDownloadCacheDirectory().getPath();
    }

//    public boolean installOtaPackage(Context context) {
//        File packageFile = new File(getSystemCachePath(context) + File.separator + mOtaFileName);
//        Log.i(LOG_TAG, "Installed OTA package path: " + packageFile.getPath());
//        Log.i(LOG_TAG, "Installed OTA package name: " + packageFile.getName());
//
//        try {
//            RecoverySystem.installPackage(context, packageFile);
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "Error while install OTA package:" + e);
//            Log.e(LOG_TAG, "Please retry download");
//            return false;
//        }
//        return true;
//    }

    /**
     * Delete single file
     *
     * @param filePath
     * @return true if file is successfully deleted, false otherwise
     */
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    public boolean deleteDirectory(String filePath) {
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }

        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }

        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                if (!deleteFile(files[i].getAbsolutePath()))
                    return false;
            } else {
                if (!deleteDirectory(files[i].getAbsolutePath()))
                    return false;
            }
        }
        return dirFile.delete();
    }

    public void deleteDownloadedFiles(Context context, String fileName) {
        deleteFile(getSystemCachePath(context) + File.separator + fileName);
        deleteFile(getSystemCachePath(context) + File.separator + "partial_downloads" + File.separator + fileName);
    }

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null) {
                return ni.isAvailable();
            }
        }
        return false;
    }


    private class NetWorkStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CONNECTIVITY_CHANGE_ACTION)) {

//                Toast.makeText(context, "Network changed: " + (mIsNetWorkConnected ? "Connected" : "Disconnected"), Toast.LENGTH_LONG).show();
            }
        }
    }

}
