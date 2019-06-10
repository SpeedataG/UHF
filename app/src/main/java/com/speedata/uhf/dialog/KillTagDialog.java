package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.bean.SpdWriteData;
import com.speedata.libuhf.interfaces.OnSpdWriteListener;
import com.speedata.libuhf.utils.StringUtils;
import com.speedata.uhf.R;

/**
 * Created by 张明_ on 2016/12/28.
 */

public class KillTagDialog extends Dialog implements
        View.OnClickListener {

    private Button Ok;
    private Button Cancle;
    private TextView EPC;
    private TextView Status;
    private EditText accessPassword;
    private EditText killPassword;
    private IUHFService iuhfService;
    private String current_tag_epc;
    private String model;
    private boolean isSuccess = false;
    private Context mContext;

    public KillTagDialog(Context context, IUHFService iuhfService
            , String current_tag_epc, String model) {
        super(context);
        // TODO Auto-generated constructor stub
        this.iuhfService = iuhfService;
        this.current_tag_epc = current_tag_epc;
        this.model = model;
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_killtag);

        Ok = (Button) findViewById(R.id.btn_lock_ok);
        Ok.setOnClickListener(this);
        Cancle = (Button) findViewById(R.id.btn_lock_cancle);
        Cancle.setOnClickListener(this);

        EPC = (TextView) findViewById(R.id.textView_lock_epc);
        EPC.setText(current_tag_epc);
        Status = (TextView) findViewById(R.id.textView_lock_status);

        accessPassword = (EditText) findViewById(R.id.editText_lock_passwd);
        killPassword = (EditText) findViewById(R.id.editText_kill_passwd);


        iuhfService.setOnWriteListener(new OnSpdWriteListener() {
            @Override
            public void getWriteData(SpdWriteData var1) {
                StringBuilder stringBuilder = new StringBuilder();
                byte[] epcData = var1.getEPCData();
                String hexString = StringUtils.byteToHexString(epcData, var1.getEPCLen());
                if (!TextUtils.isEmpty(hexString)) {
                    stringBuilder.append("EPC：" + hexString + "\n");
                }
                if (var1.getStatus() == 0) {
                    //状态判断，已经写卡成功了就不返回错误码了
                    isSuccess = true;
                    stringBuilder.append(mContext.getResources().getString(R.string.Status_Write_Card_Ok) + "\n");
                    handler.sendMessage(handler.obtainMessage(1, stringBuilder));
                } else {
                    stringBuilder.append(mContext.getResources().getString(R.string.Status_Write_Card_Faild) + var1.getStatus() + "\n");
                }
                if (!isSuccess) {
                    handler.sendMessage(handler.obtainMessage(1, stringBuilder));
                }

            }
        });
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == Ok) {
            final String accessPwd = accessPassword.getText().toString();
            final String killPwd = killPassword.getText().toString();
            if (TextUtils.isEmpty(accessPwd) || TextUtils.isEmpty(killPwd)) {
                Toast.makeText(mContext, "密码不能为空", Toast.LENGTH_LONG).show();
            }
            Status.setText(R.string.locking_card);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int reval = iuhfService.setKill(accessPwd, killPwd);
                    if (reval != 0) {
                        handler.sendMessage(handler.obtainMessage(1, mContext.getResources().getString(R.string.param_error)));
                    }
                }
            }).start();
        } else if (v == Cancle) {
            dismiss();
        }
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Status.setText(msg.obj + "");
            }
        }
    };
}
