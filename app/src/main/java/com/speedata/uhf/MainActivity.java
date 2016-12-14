package com.speedata.uhf;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;

public class MainActivity extends AppCompatActivity {

    private IUHFService iuhfService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iuhfService = UHFManager.getUHFService();
        iuhfService.OpenDev();
        iuhfService.inventory_start(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                System.out.println("===handleMessage==");
                super.handleMessage(msg);
            }
        });
    }
}
