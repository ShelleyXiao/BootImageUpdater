package com.zx.ott.bootimage.utils;

import android.util.Xml;

import com.zx.ott.bootimage.UpdateBootImageService;
import com.zx.ott.bootimage.UpdateInfo;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ShaudXiao
 * Date: 2017-06-27
 * Time: 16:56
 * Company: zx
 * Description:
 * FIXME
 */


public class XmlUtils {
    public static Map<UpdateBootImageService.DownloadFileType, UpdateInfo> loadOtaPackageInfo(InputStream is) {
        Map<UpdateBootImageService.DownloadFileType, UpdateInfo> infoList = null;
        UpdateInfo info = null;
        if(null == is) {
            return null;
        }
        try {
//            InputStream inputStream = new FileInputStream(new File(filePath));
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(is, "utf-8");

            int eventType = xmlPullParser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT) {
                switch(eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        infoList = new HashMap<>();
                        break;
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("firmwareudpate")) {

                        } else if (xmlPullParser.getName().equals("animation")
                                || xmlPullParser.getName().equals("bootlogo")
                                || xmlPullParser.getName().equals("video")) {
                            Logger.getLogger().i("name = " + xmlPullParser.getName());
                            info = new UpdateInfo();
                            String ver = xmlPullParser.getAttributeValue(0);
                            info.setVersion(ver);
                            eventType = xmlPullParser.next();
                        } else if (xmlPullParser.getName().equals("name")) {
                            String name = xmlPullParser.nextText();
                            info.setName(name);
                            eventType = xmlPullParser.next();
                        } else if (xmlPullParser.getName().equals("size")) {
                            String size = xmlPullParser.nextText();
                            info.setSize(Integer.valueOf(size));
                            eventType = xmlPullParser.next();
                        } else if (xmlPullParser.getName().equals("desc_en")) {
                            String desc = xmlPullParser.nextText();
                            info.setDescEn(desc);
                            eventType = xmlPullParser.next();
                        } else if (xmlPullParser.getName().equals("md5")) {
                            String md5 = xmlPullParser.nextText();
                            info.setMd5(md5);
                            eventType = xmlPullParser.next();
                        } else if (xmlPullParser.getName().equals("downloadurl")) {
                            String url = xmlPullParser.nextText();
                            info.setDownloadUrl(url);
                            eventType = xmlPullParser.next();
                        } else if (xmlPullParser.getName().equals("level")) {
                            String level = xmlPullParser.nextText();
                            info.setLevel(Integer.parseInt(level));
                            eventType = xmlPullParser.next();
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        Logger.getLogger().d(info != null ? info.toString() : " ");
                        if (xmlPullParser.getName().equals("bootlogo")) {
                            infoList.put(UpdateBootImageService.DownloadFileType.JPG, info);
                            info = null;
                        } else  if (xmlPullParser.getName().equals("animation")) {
                            infoList.put(UpdateBootImageService.DownloadFileType.ZIP, info);
                            info = null;
                        } else  if (xmlPullParser.getName().equals("video")) {
                            infoList.put(UpdateBootImageService.DownloadFileType.MP4, info);
                            info = null;
                        }
                        break;
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        return infoList;
    }
}
