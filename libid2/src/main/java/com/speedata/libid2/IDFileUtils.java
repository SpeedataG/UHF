package com.speedata.libid2;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by brxu on 2016/12/19.
 */

public class IDFileUtils {

    public static boolean isExit() {
        File files = new File("/sdcard/wltlib");

        if (!files.exists()) {
            return true;
        } else return false;
    }

    public static void copyfile(String fileDirPath, String fileName, int id, Context mContext) {
        String filePath = fileDirPath + "/" + fileName;// 文件路径
        try {

            File files = new File("/sdcard/wltlib");

            if (!files.exists()) {
                files.mkdirs();

            }

            // 文件夹存在，则将apk中raw文件夹中的须要的文档拷贝到该文件夹下
            File file = new File(filePath);
            if (!file.exists()) {// 文件不存在
                InputStream is = mContext.getResources().openRawResource(
                        id);// 通过raw得到数据资源
                FileOutputStream fs = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int count = 0;// 循环写出
                while ((count = is.read(buffer)) > 0) {
                    fs.write(buffer, 0, count);
                }
                fs.close();// 关闭流
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
