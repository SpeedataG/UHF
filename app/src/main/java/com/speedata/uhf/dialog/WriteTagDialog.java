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

import static android.content.ContentValues.TAG;

/**
 * Created by 张明_ on 2016/12/27.
 */

public class WriteTagDialog extends Dialog implements
        android.view.View.OnClickListener {

    private Button Ok;
    private Button Cancle;
    private TextView EPC;
    private TextView Status;
    private EditText Write_Addr;
    private EditText Write_Count;
    private EditText Write_Passwd;
    private IUHFService iuhfService;
    private Context mContext;
    private int which_choose;
    private String str_content;
    private String current_tag_epc;
    private String model;

    public WriteTagDialog(Context context,IUHFService iuhfService,
                          String str_content,int which_choose,String current_tag_epc,String model) {
        super(context);
        this.iuhfService=iuhfService;
        this.mContext=context;
        this.which_choose=which_choose;
        this.str_content=str_content;
        this.current_tag_epc=current_tag_epc;
        this.model=model;
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write);

        Ok = (Button) findViewById(R.id.btn_write_ok);
        Ok.setOnClickListener(this);
        Cancle = (Button) findViewById(R.id.btn_write_cancle);
        Cancle.setOnClickListener(this);

        EPC = (TextView) findViewById(R.id.textView_write_epc);
        EPC.setText( current_tag_epc);
        Status = (TextView) findViewById(R.id.textView_write_status);

        Write_Addr = (EditText) findViewById(R.id.editText_write_addr);
        Write_Count = (EditText) findViewById(R.id.editText_write_count);
        Write_Passwd = (EditText) findViewById(R.id.editText_write_passwd);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == Ok) {
            final String str_addr = Write_Addr.getText().toString();
            final String str_count = Write_Count.getText().toString();
            final String str_passwd = Write_Passwd.getText().toString();
            Status.setText("正在写卡中....");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "write start: "+System.currentTimeMillis());
                    int rev=iuhfService.write_area(which_choose,str_addr,str_passwd,str_count
                            ,str_content);
                    Log.d(TAG, "write end: "+System.currentTimeMillis());
                    Message message=new Message();
                    message.what=1;
                    message.obj=rev;
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
                int rev= (int) msg.obj;
                if (rev == 0) {
                    EventBus.getDefault().post(new MsgEvent("write_Status","" ));
                    dismiss();
                } else if (rev == -1) {
                    Status.setText(R.string.Status_Write_Error);
                } else if (rev == -2) {
                    Status.setText(R.string.Status_Content_Length_Error);
                } else if (rev == -3) {
                    Status.setText(R.string.Status_InvalidNumber);
                }else {
                    Status.setText(R.string.Status_Write_Error);
                }
            }
        }
    };
}