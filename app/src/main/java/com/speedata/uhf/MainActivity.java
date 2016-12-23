package com.speedata.uhf;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.speedata.libuhf.IUHFService;

public class MainActivity extends AppCompatActivity {

    private IUHFService iuhfService;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        testUHF();
    }



    private void testUHF() {
//        iuhfService = UHFManager.getUHFService();
//        iuhfService.OpenDev();
//        iuhfService.inventory_start(new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                ArrayList<XinLianQilian.Tag_Data> cx = (ArrayList<XinLianQilian.Tag_Data>) msg.obj;
////                System.out.println("===handleMessage==");
//                super.handleMessage(msg);
//            }
//        });
    }
//    private IUHFService iuhfService;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        iuhfService = UHFManager.getUHFService();
//        iuhfService.OpenDev();
//        iuhfService.inventory_start(new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                ArrayList<XinLianQilian.Tag_Data> cx = (ArrayList<XinLianQilian.Tag_Data>) msg.obj;
////                System.out.println("===handleMessage==");
//                super.handleMessage(msg);
//            }
//        });
//    }
}
