package com.speedata.uhf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.speedata.libutils.DataConversionUtils;


public class StartService extends BroadcastReceiver {
    private static final String START_SERVICE = "com.spd.action.start_server";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        assert action != null;
        if (action.equals(START_SERVICE)){
//            context.startService(new Intent(context,MyService.class));
            Log.d("UHFService","===BroadcastReceiver===startService===");
        }
    }

}
