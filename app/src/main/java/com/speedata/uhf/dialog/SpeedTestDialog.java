package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.speedata.libuhf.IUHFService;
import com.speedata.uhf.R;

/**
 * Created by 张明_ on 2016/12/27.
 */

public class SpeedTestDialog extends Dialog implements android.view.View.OnClickListener {
    private Button st;
    private TextView tv;
    private boolean inth = false;
    //private Thread thr;
    private Handler hd;
    private long total = 0;
    private long time0 = 0, time1 = 0;
    IUHFService iuhfService;

    public SpeedTestDialog(Context context,IUHFService iuhfService) {
        super(context);
        this.iuhfService=iuhfService;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spt);

        st = (Button) findViewById(R.id.button_spt_b);
        st.setOnClickListener(this);

        tv = (TextView) findViewById(R.id.textView_spt);

        hd = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    if (inth) {
                        total++;
                        tv.setText("search " + total + " times");
                    }
                }
            }
        };

        iuhfService.reg_handler(hd);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == st) {
            if (inth) {
                iuhfService.inventory_stop();
                time1 = SystemClock.uptimeMillis();
                this.setCancelable(true);
                st.setText("start");
                inth = false;
                long dv = time1 - time0;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                tv.setText("search cards " + total * 1000 / dv + " times / s");
                Log.d("kt45", "in " + dv + "ms valid search " + total + " times average " + total * 1000 / dv + " times per ms");
            } else {
                iuhfService.inventory_start();
                time0 = SystemClock.uptimeMillis();
                this.setCancelable(false);
                total = 0;
                st.setText("stop");
                inth = true;
            }
        }
    }

}
