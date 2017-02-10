package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.speedata.libuhf.IUHFService;
import com.speedata.uhf.MsgEvent;
import com.speedata.uhf.R;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by 张明_ on 2016/12/28.
 */

public class ReadTagDialog extends Dialog implements
        android.view.View.OnClickListener {

    private Button Ok;
    private Button Cancle;
    private TextView EPC;
    private TextView Status;
    private EditText Read_Addr;
    private EditText Read_Count;
    private EditText Password;
    private IUHFService iuhfService;
    private String current_tag_epc;
    private int which_choose;
    private String model;
    private Context mContext;

    public ReadTagDialog(Context context,IUHFService iuhfService
            ,int which_choose,String current_tag_epc,String model) {
        super(context);
        // TODO Auto-generated constructor stub
        this.iuhfService=iuhfService;
        this.current_tag_epc=current_tag_epc;
        this.which_choose=which_choose;
        this.model=model;
        this.mContext=context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read);

        Ok = (Button) findViewById(R.id.btn_read_ok);
        Ok.setOnClickListener(this);
        Cancle = (Button) findViewById(R.id.btn_read_cancle);
        Cancle.setOnClickListener(this);

        EPC = (TextView) findViewById(R.id.textView_read_epc);
        EPC.setText(current_tag_epc);
        Status = (TextView) findViewById(R.id.textView_read_status);

        Read_Addr = (EditText) findViewById(R.id.editText_read_addr);
        Read_Count = (EditText) findViewById(R.id.editText_read_count);
        Password = (EditText) findViewById(R.id.editText_rp);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == Ok) {
            final String str_addr = Read_Addr.getText().toString();
            final String str_count = Read_Count.getText().toString();
            final String str_passwd = Password.getText().toString();
            Status.setText("正在读卡中....");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String res = iuhfService.read_area(which_choose,str_addr,str_count,str_passwd);
                    Message message=new Message();
                    if (res == null) {
                        message.what=1;
                        handler.sendMessage(message);
                    } else {
                        message.what=2;
                        message.obj=res;
                        handler.sendMessage(message);
                    }
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
            switch (msg.what){
                case 1:
                    Status.setText(R.string.Status_Read_Card_Faild);
                    break;
                case 2:
                    EventBus.getDefault().post(new MsgEvent("read_Status",msg.obj.toString()));
                    dismiss();
                    break;
            }
        }
    };
}
