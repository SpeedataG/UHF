package com.speedata.uhf.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.WindowManager;

import com.speedata.libuhf.UHFManager;
import com.speedata.libuhf.interfaces.OnSpdBanMsgListener;

import java.util.Objects;

/**
 * @author zzc
 */
public class AlertDialogManager {

    private static Context mContext;
    private static AlertDialogManager alertDialogManager;
    private static AlertDialog alertDialog;

    private AlertDialogManager() {

    }

    private AlertDialogManager(Context mContext) {
        AlertDialogManager.mContext = mContext;
    }

    public static AlertDialogManager getAlertDialogManager() {
        return alertDialogManager;
    }

    public static AlertDialogManager getAlertInstance(Context mContext) {
        if (getAlertDialogManager() == null) {
            alertDialogManager = new AlertDialogManager(mContext);
            setBuilder();
        }
        return alertDialogManager;
    }

    private static void setBuilder() {
        UHFManager uhfManager = new UHFManager();
        uhfManager.setOnBanMsgListener(new OnSpdBanMsgListener() {
            @Override
            public void getBanMsg(String var1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(var1);
                builder.setMessage("请按确定键关闭超高频");
                builder.setCancelable(true);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        System.exit(0);
                    }
                });
                //对话框显示的监听事件
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Log.e("UHFService", "对话框显示了");
                        UHFManager.closeUHFService();
                    }
                });
                alertDialog = builder.create();
                alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                alertDialog.show();
            }
        });
    }
}
