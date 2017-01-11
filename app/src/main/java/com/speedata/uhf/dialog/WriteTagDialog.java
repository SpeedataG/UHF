package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.speedata.libuhf.IUHFService;
import com.speedata.uhf.MsgEvent;
import com.speedata.uhf.R;

import org.greenrobot.eventbus.EventBus;

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
            String str_addr = Write_Addr.getText().toString();
            String str_count = Write_Count.getText().toString();
            String str_passwd = Write_Passwd.getText().toString();

//            int num_addr;
//            int num_count;
//            long passwd;
//            try {
//                num_addr = Integer.parseInt(str_addr, 16);
//                num_count = Integer.parseInt(str_count, 10);
//                if (model.equals("FEILIXIN")) {
//                    passwd = Long.parseLong(str_passwd);
//                } else {
//                    passwd = Long.parseLong(str_passwd, 16);
//                }
//
//            } catch (NumberFormatException p) {
//                Status.setText(R.string.Status_InvalidNumber);
//                return;
//            }
//            int rev = write_card(which_choose, num_addr, num_count * 2,
//                    (int) passwd, str_content);


            int rev=iuhfService.write_area(which_choose,str_addr,str_passwd,str_count
                    ,str_content);

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

        } else if (v == Cancle) {
            dismiss();
        }
    }


//    private int write_card(int area, int addr, int count, int passwd, String cnt) {
//        byte[] cf;
//        if (model.equals("FEILIXIN")) {
//            cf = getBytes(cnt);
//        } else {
//            StringTokenizer cn = new StringTokenizer(cnt);
//            if (cn.countTokens() < count) {
//                return -3;
//            }
//            cf = new byte[count];
//            int index = 0;
//            while (cn.hasMoreTokens() && (index < count)) {
//                try {
//                    int k = Integer.parseInt(cn.nextToken(), 16);
//                    if (k > 0xff) {
//                        throw new NumberFormatException("can't bigger than 0xff");
//                    }
//                    cf[index++] = (byte) k;
//                } catch (NumberFormatException p) {
//                    return -4;
//                }
//            }
//        }
//        return iuhfService.write_area(area, addr, passwd, cf);
//    }

    /**
     * 将一个可能带空格的字符串，以Byte.parseByte的方法转化为byte数组
     */
//    private byte[] getBytes(String data) {
//        String newData = data.trim().replace(" ", "");
//        byte[] datas = new byte[newData.length()];
//        int i;
//        for (i = 0; i < datas.length; ++i) {
//            datas[i] = Byte.parseByte(newData.substring(i, i + 1), 16);
//        }
//        return datas;
//    }
}