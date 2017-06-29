package com.zx.ott.bootimage;

/**
 * User: ShaudXiao
 * Date: 2017-06-27
 * Time: 18:56
 * Company: zx
 * Description:
 * FIXME
 */


public class UpdateInfo {

    private String mVersion;
    private Integer mNum;
    private String mName;
    private String mDescCn;
    private String mDescTw;
    private String mDescEn;
    private String mMd5;
    private Integer mSize;
    private Integer mLevel;
    private boolean mNeedBackup;
    private String mDownloadUrl;

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String ver) {
        this.mVersion = ver;
    }

    public Integer getNum() {
        return mNum;
    }

    public void setNum(Integer num) {
        this.mNum = num;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getDescCn() {
        return mDescCn;
    }

    public void setDescCn(String desc) {
        this.mDescCn = desc;
    }

    public String getDescTw() {
        return mDescTw;
    }

    public void setDescTw(String desc) {
        this.mDescTw = desc;
    }

    public String getDescEn() {
        return mDescEn;
    }

    public void setDescEn(String desc) {
        this.mDescEn = desc;
    }

    public String getMd5() {
        return mMd5;
    }

    public void setMd5(String md5) {
        this.mMd5 = md5;
    }

    public Integer getSize() {
        return mSize;
    }

    public void setSize(Integer size) {
        this.mSize = size;
    }

    public Integer getLevel() {
        return mLevel;
    }

    public void setLevel(Integer level) {
        this.mLevel = level;
    }

    public boolean getNeedBackup() {
        return mNeedBackup;
    }

    public void setNeedBackup(boolean needBackup) {
        this.mNeedBackup = needBackup;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.mDownloadUrl = downloadUrl;
    }

    @Override
    public String toString() {
        return "UpdateInfo{" +
                "mVersion='" + mVersion + '\'' +
                ", mNum=" + mNum +
                ", mName='" + mName + '\'' +
                ", mDescCn='" + mDescCn + '\'' +
                ", mDescTw='" + mDescTw + '\'' +
                ", mDescEn='" + mDescEn + '\'' +
                ", mMd5='" + mMd5 + '\'' +
                ", mSize=" + mSize +
                ", mLevel=" + mLevel +
                ", mNeedBackup=" + mNeedBackup +
                ", mDownloadUrl='" + mDownloadUrl + '\'' +
                '}';
    }
}
