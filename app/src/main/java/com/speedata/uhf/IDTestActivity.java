package com.speedata.uhf;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.serialport.DeviceControl;
import android.serialport.SerialPort;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.speedata.libid2.IDInfor;
import com.speedata.libid2.IDManager;
import com.speedata.libid2.IDReadCallBack;
import com.speedata.libid2.IID2Service;

import java.io.IOException;

public class IDTestActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int WIDTH = 30;
    public static final int HEIGHT = 34;
    private TextView tvIDInfor;
    private ImageView imgPic;
    private Button btnRead, btnSelect, btnSearch;
    private Button btnGet;
    private ImageView imgFinger;
    private CheckBox checkBoxFinger;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            IDInfor idInfor1 = (IDInfor) msg.obj;
            if (idInfor1.isSuccess()) {
                tvIDInfor.setText("NAME:" + idInfor1.getName());
                imgPic.setImageBitmap(idInfor1.getBmps());
            } else {
                tvIDInfor.setText("ERROR:" + idInfor1.getErrorMsg());
            }
            if (idInfor1.isWithFinger()) {

                Bitmap bitmap = ShowFingerBitmap(idInfor1.getFingerprStringer(), WIDTH, HEIGHT);
                imgFinger.setImageBitmap(bitmap);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idtest);
        tvIDInfor = (TextView) findViewById(R.id.tv_idinfor);
        imgPic = (ImageView) findViewById(R.id.img_pic);
        btnRead = (Button) findViewById(R.id.btn_read);
        btnRead.setOnClickListener(this);
        btnSearch = (Button) findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(this);
        btnSelect = (Button) findViewById(R.id.btn_select);
        btnSelect.setOnClickListener(this);
        btnGet = (Button) findViewById(R.id.btn_get);
        btnGet.setOnClickListener(this);
        checkBoxFinger = (CheckBox) findViewById(R.id.checkbox_wit_finger);
        imgFinger = (ImageView) findViewById(R.id.img_finger);
        initID();
    }


    private IID2Service iid2Service;

    private void testID() {
        iid2Service.getIDInfor(checkBoxFinger.isChecked());
    }

    private void initID() {
        iid2Service = IDManager.getInstance();
        try {
            iid2Service.initDev(this, new IDReadCallBack() {
                @Override
                public void callBack(IDInfor infor) {
                    Message message = new Message();
                    message.obj = infor;
                    handler.sendMessage(message);
                }
            }, SerialPort.SERIAL_TTYMT1, 115200, DeviceControl.PowerType.MAIN_AND_EXPAND
                    , 88,6);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View view) {
        if (view == btnRead) {
            IDInfor infor = iid2Service.readCard(checkBoxFinger.isChecked());

            Message message = new Message();
            message.obj = infor;
            handler.sendMessage(message);

        } else if (view == btnGet) {
            tvIDInfor.setText("");
            imgPic.setImageDrawable(null);
            testID();
        } else if (view == btnSearch) {
            int result = iid2Service.searchCard();
            tvIDInfor.setText(iid2Service.parseReturnState(result));
        } else if (view == btnSelect) {
            int result = iid2Service.selectCard();
            tvIDInfor.setText(iid2Service.parseReturnState(result));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            iid2Service.releaseDev();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //------------------------------------------------------------------------------------------
    private Bitmap ShowFingerBitmap(byte[] image, int width, int height) {
        if (width == 0) return null;
        if (height == 0) return null;

        int[] RGBbits = new int[width * height];
//        viewFinger.invalidate();
        for (int i = 0; i < width * height; i++) {
            int v;
            if (image != null) v = image[i] & 0xff;
            else v = 0;
            RGBbits[i] = Color.rgb(v, v, v);
        }
        Bitmap bmp = Bitmap.createBitmap(RGBbits, width, height, Bitmap.Config.RGB_565);
        return bmp;
    }
}
