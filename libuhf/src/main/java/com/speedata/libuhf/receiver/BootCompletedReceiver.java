package com.speedata.libuhf.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.speedata.libuhf.service.GetModleService;

/**
 * Created by 张明_ on 2016/12/19.
 */

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent();
        myIntent.setAction("com.speedata.libuhf.service.GetModleService");
        myIntent.setClass(context,GetModleService.class);
        context.startService(myIntent);
    }
}
