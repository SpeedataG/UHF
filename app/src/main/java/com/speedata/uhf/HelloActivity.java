package com.speedata.uhf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;
import com.speedata.libuhf.bean.SpdInventoryData;
import com.speedata.libuhf.bean.SpdReadData;
import com.speedata.libuhf.bean.SpdWriteData;
import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.libuhf.interfaces.OnSpdReadListener;
import com.speedata.libuhf.interfaces.OnSpdWriteListener;
import com.speedata.libuhf.utils.DataConversionUtils;
import com.speedata.libuhf.utils.SharedXmlUtil;

import java.io.IOException;

/**
 * @author zzc
 */
public class HelloActivity extends Activity {

    private IUHFService iuhfService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello);
        //获取实例对象
        try {
            iuhfService = UHFManager.getUHFService(this);
        } catch (Exception e) {
            //获取对象失败，未识别模块信息
            e.printStackTrace();
        }
        //添加盘点监听
        iuhfService.setOnInventoryListener(new OnSpdInventoryListener() {
            @Override
            public void getInventoryData(SpdInventoryData spdInventoryData) {
                //TODO 盘点成功回调
            }

            @Override
            public void onInventoryStatus(int i) {
                //TODO 盘点失败回调
            }
        });

        //添加读监听
        iuhfService.setOnReadListener(new OnSpdReadListener() {
            @Override
            public void getReadData(SpdReadData spdReadData) {
                //TODO 读卡回调
                if (spdReadData.getStatus() == 0) {
                    //读卡成功
                } else {
                    //读卡失败
                }
            }
        });

        //添加写监听
        iuhfService.setOnWriteListener(new OnSpdWriteListener() {
            @Override
            public void getWriteData(SpdWriteData spdWriteData) {
                //TODO 写卡回调
                if (spdWriteData.getStatus() == 0) {
                    //写卡成功
                } else {
                    //写卡失败
                }
            }
        });

        //模块上电
        if (iuhfService.openDev() != 0) {
            //TODO 上电失败
        }
    }

    /**
     * 开始盘点 点击事件
     *
     * @param view
     */
    public void invStart(View view) {
        iuhfService.inventoryStart();
    }

    /**
     * 停止盘点 点击事件
     *
     * @param view
     */
    public void invStop(View view) {
        iuhfService.inventoryStop();
    }

    /**
     * 读卡 点击事件
     *
     * @param view
     */
    public void readTag(View view) {
        int res = iuhfService.readArea(1, 0, 6, "00000000");
        if (res != 0) {
            //TODO 参数错误
        }
    }

    /**
     * 写卡 点击事件
     *
     * @param view
     */
    public void writeTag(View view) {
        String hexString = "FFFF";
        byte[] content = DataConversionUtils.hexStringToByteArray(hexString);
        int count = content.length / 2;
        int res = iuhfService.writeArea(1, 2, count, "00000000", content);
        if (res != 0) {
            //TODO 参数错误
        }
    }

    @Override
    protected void onDestroy() {
        if (iuhfService != null) {
            //模块下电
            iuhfService.closeDev();
        }
        //释放资源
        UHFManager.closeUHFService();
        super.onDestroy();
    }
}
