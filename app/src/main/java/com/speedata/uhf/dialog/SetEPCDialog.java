package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.speedata.libuhf.IUHFService;
import com.speedata.uhf.MsgEvent;
import com.speedata.uhf.R;

import org.greenrobot.eventbus.EventBus;

import java.util.StringTokenizer;

/**
 * Created by 张明_ on 2016/12/28.
 */

public class SetEPCDialog extends Dialog implements
        android.view.View.OnClickListener {

    private Button Ok;
    private Button Cancle;
    private TextView EPC;
    private TextView Status;
    private EditText passwd;
    private EditText newepc;
    private EditText newepclength;
    private IUHFService iuhfService;
    private String current_tag_epc;

    public SetEPCDialog(Context context,IUHFService iuhfService,String current_tag_epc) {
        super(context);
        // TODO Auto-generated constructor stub
        this.iuhfService=iuhfService;
        this.current_tag_epc=current_tag_epc;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setepc);

        Ok = (Button) findViewById(R.id.btn_epc_ok);
        Ok.setOnClickListener(this);
        Cancle = (Button) findViewById(R.id.btn_epc_cancle);
        Cancle.setOnClickListener(this);

        EPC = (TextView) findViewById(R.id.textView_epc_epc);
        EPC.setText(current_tag_epc);
        Status = (TextView) findViewById(R.id.textView_epc_status);

        passwd = (EditText) findViewById(R.id.editText_epc_passwd);
        newepc = (EditText) findViewById(R.id.editText_epc_newepc);
        newepclength = (EditText) findViewById(R.id.editText_epc_epclength);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == Ok) {
            final String password = passwd.getText().toString();
            String epc_str = newepc.getText().toString();
            String count_str = newepclength.getText().toString();
            final int epcl;
            try {
                epcl = Integer.parseInt(count_str, 10);
            } catch (NumberFormatException e) {
                Log.e("as3992", "parse int error in set epc");
                return;
            }

            StringTokenizer sepc = new StringTokenizer(epc_str);
            if (epcl > sepc.countTokens()) {
                Status.setText(R.string.Status_Content_Length_Error);
                return;
            }

            final byte[] eepc = new byte[sepc.countTokens()];

            int index = 0;
            while (sepc.hasMoreTokens()) {
                try {
                    int ak = Integer.parseInt(sepc.nextToken(), 16);
                    if (ak > 0xff) {
                        throw new NumberFormatException("Can't bigger than 0xff");
                    }
                    eepc[index++] = (byte) ak;
                } catch (NumberFormatException p) {
                    Log.e("as3992", "parse int error in set epc: " + p.getMessage());
                    Status.setText("Invalid epc str");
                    return;
                }
            }
            Status.setText("正在写卡中....");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int reval = set_EPC(epcl, password, eepc);
                    Message message=new Message();
                    message.what=1;
                    message.obj=reval;
                    handler.sendMessage(message);
                }
            }).start();



        } else if (v == Cancle) {
            dismiss();
        }
    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what==1){
                int reval= (int) msg.obj;
                if (reval == 0) {
                    EventBus.getDefault().post(new MsgEvent("SetEPC_Status",""));
                    dismiss();
                } else if (reval == -1) {
                    Status.setText(R.string.Status_Write_Error);
                } else if (reval == -2) {
                    Status.setText(R.string.Status_Passwd_Length_Error);
                } else if (reval == -3) {
                    Status.setText(R.string.Status_Content_Length_Error);
                } else if (reval == -4) {
                    Status.setText(R.string.Status_InvalidNumber);
                } else if (reval == -5) {
                    Status.setText(R.string.Status_Read_Pc_Error);
                }
            }
        }
    };
    int set_EPC(int epclength, String passwd, byte[] EPC) {
        byte[] res;
        long pss = 0;
        if (epclength > 31) {
            return -3;
        }
        if (epclength * 2 < EPC.length) {
            return -3;
        }
        try {
            pss = Long.parseLong(passwd, 16);
        } catch (NumberFormatException e) {
            return -4;
        }
        res = iuhfService.read_area(iuhfService.EPC_A, 1, 2, 0);
        if (res == null) {
            return -5;
        }
        res[0] = (byte) ((res[0] & 0x7) | (epclength << 3));
        byte[] f = new byte[2 + epclength * 2];
        try {
            System.arraycopy(res, 0, f, 0, 2);
            System.arraycopy(EPC, 0, f, 2, epclength * 2);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return iuhfService.write_area(iuhfService.EPC_A, 1, (int) pss, f);
    }
}
