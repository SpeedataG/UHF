package com.speedata.uhf;

import android.app.smallscreen.SmallScreenManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class SmallScreenActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnWriteCH, btnWriteEN, btnClear;
    private ToggleButton toggleTime;
    private SmallScreenManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_small_screen);
        btnClear = (Button) findViewById(R.id.btn_clear);
        btnWriteCH = (Button) findViewById(R.id.btn_write_ch);
        btnWriteEN = (Button) findViewById(R.id.btn_write_en);
        toggleTime = (ToggleButton) findViewById(R.id.btn_sync_time);
        btnClear.setOnClickListener(this);
        btnWriteCH.setOnClickListener(this);
        btnWriteEN.setOnClickListener(this);
        manager = SmallScreenManager.getInstance();
//        manager = (SmallScreenManager) this.getSystemService("smallscreen");
        toggleTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    if (b)
                        manager.startClock();
                    else
                        manager.stopClock();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
//        try {
//            manager.writeBuffer(11,11);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public void onClick(View view) {
//        if (view == btnClear) {
//            try {
//                manager.clearScreen();
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//        } else if (view == btnWriteCH) {
//
//            String tem = "你好许栢茹";
//            try {
////                manager.stopClock();
//                byte[] gb2312s = tem.getBytes("gb2312");
//                manager.writeGb2312Buffer(gb2312s, gb2312s.length);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        } else if (view == btnWriteEN) {
//            String tem = "Speedata";
//            try {
////                manager.stopClock();
//                byte[] gb2312s = tem.getBytes("gb2312");
//                manager.writeAsciiBuffer(gb2312s, gb2312s.length);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        }
    }
}
