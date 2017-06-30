package com.zx.ott.bootimage;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.zx.ott.bootimage.utils.Constant;
import com.zx.ott.bootimage.utils.Logger;

import java.io.File;


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



    private CheckBoxPreference mUpdatebootanimation;
    private CheckBoxPreference mUpdatebootlogo;
    private CheckBoxPreference mUpdatebootVideo;

    private boolean checkedAnimation;
    private boolean checkedBootlogo;
    private boolean checkBootVideo;

    private Button mCheckButton;


    private boolean mIsCheckingCanceled = false;

    private NetWorkStatusReceiver mNetworkChangeReceiver1;

    private SharedPreferences mSharedPrefs;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.system_update);
        setContentView(R.layout.system_update_layout);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);


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
    }


    private void updateCheckValue() {
        checkedAnimation = mUpdatebootanimation.isChecked();
        checkBootVideo = mUpdatebootVideo.isChecked();
        checkedBootlogo = mUpdatebootlogo.isChecked();

        Logger.getLogger().d("checkedAnimation =  " + checkedAnimation + " checkBootVideo = " + checkBootVideo
                        + " checkedBootlogo = " + checkedBootlogo);
    }


    private void checkUpdate() {

        if (!isNetworkConnected(this)) {

        } else {
            checkUpdateInfo();
        }
    }

    private void checkUpdateInfo() {

        updateCheckValue();

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
            mUpdatebootanimation.setChecked(Boolean.valueOf(prefsValue));
        } else if (preference.getKey().equals(KEY_UPDATE_BOOTLOGO)) {
            Log.e(LOG_TAG, "KEY_UPDATE_BOOTLOGO checked: " + String.valueOf(objValue));
            String prefsValue = objValue.toString();
            mUpdatebootlogo.setChecked(Boolean.valueOf(prefsValue));
        } else if (preference.getKey().equals(KEY_UPDATE_VENDOR_VIDEO)) {
            Log.e(LOG_TAG, "KEY_UPDATE_VENDOR_VIDEO is checked: " + String.valueOf(objValue));
            String prefsValue = objValue.toString();
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






    private void showToast(String arg) {
        Toast.makeText(this, arg, Toast.LENGTH_SHORT).show();
    }



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
