package com.zx.ott.bootimage.downloader.downmanger.bizs;

class DLThreadInfo {
    String id;
    String baseUrl;
    int start, end;
    boolean isStop;

    DLThreadInfo(String id, String baseUrl, int start, int end) {
        this.id = id;
        this.baseUrl = baseUrl;
        this.start = start;
        this.end = end;
    }
}