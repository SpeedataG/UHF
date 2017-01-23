package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.speedata.libuhf.IUHFService;
import com.speedata.uhf.MsgEvent;
import com.speedata.uhf.R;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by 张明_ on 2016/12/28.
 */

public class SetPasswordDialog extends Dialog implements
        android.view.View.OnClickListener {

    private String[] passwd_list = {"Kill Password", "Access Password"};
    private Button Ok;
    private Button Cancle;
    private TextView EPC;
    private TextView Status;
    private Spinner area_select;
    private EditText access_passwd;
    private EditText new_passwd;
    private ArrayAdapter<String> setadapter;
    private IUHFService iuhfService;
    private String current_tag_epc;
    private String model;

    public SetPasswordDialog(Context context, IUHFService iuhfService
            , String current_tag_epc, String model) {
        super(context);
        // TODO Auto-generated constructor stub
        this.iuhfService = iuhfService;
        this.current_tag_epc = current_tag_epc;
        this.model = model;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setpassword);

        Ok = (Button) findViewById(R.id.btn_setpawd_ok);
        Ok.setOnClickListener(this);
        Cancle = (Button) findViewById(R.id.btn_setpawd_cancle);
        Cancle.setOnClickListener(this);

        EPC = (TextView) findViewById(R.id.textView_setpawd_epc);
        EPC.setText(current_tag_epc);
        Status = (TextView) findViewById(R.id.textView_setpawd_status);

        access_passwd = (EditText) findViewById(R.id.editText_setpawd_accesspd);
        new_passwd = (EditText) findViewById(R.id.editText_setpawd_newpd);

        setadapter = new ArrayAdapter<String>(this.getContext(),
                android.R.layout.simple_spinner_item, passwd_list);
        setadapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        area_select = (Spinner) findViewById(R.id.spinner_setpawd_paswd);
        area_select.setAdapter(setadapter);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == Ok) {
            String cur_pass = access_passwd.getText().toString();
            String new_pass = new_passwd.getText().toString();
            int which = area_select.getSelectedItemPosition();
            int reval = iuhfService.set_Password(which, cur_pass,
                    new_pass);
            if (reval == 0) {
                EventBus.getDefault().post(new MsgEvent("setPWD_Status", ""));
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
                Status.setText(R.string.Status_Wrong_Password_Type);
            }
        } else if (v == Cancle) {
            dismiss();
        }
    }

//    int set_Password(int which, String cur_pass, String new_pass) {
//        if (which > 1 || which < 0) {
//            return -5;
//        }
//        if (model.equals("FEILIXIN")) {
//            long cp = Long.parseLong(cur_pass);
//            byte[] nps = getBytes(new_pass);
//            return iuhfService.write_area(iuhfService.RESERVED_A, which * 2, (int) cp, nps);
//        } else {
//            try {
//                long cp = Long.parseLong(cur_pass, 16);
//                if ((cp > 0xffffffffL) || (cp < 0)) {
//                    throw new NumberFormatException("can't bigger than 0xffffffff");
//                }
//                long np = Long.parseLong(new_pass, 16);
//                if ((np > 0xffffffffL) || (np < 0)) {
//                    throw new NumberFormatException("can't bigger than 0xffffffff");
//                }
//
//                byte[] nps = new byte[4];
//                nps[3] = (byte) ((np >> 0) & 0xff);
//                nps[2] = (byte) ((np >> 8) & 0xff);
//                nps[1] = (byte) ((np >> 16) & 0xff);
//                nps[0] = (byte) ((np >> 24) & 0xff);
//                return iuhfService.write_area(iuhfService.RESERVED_A, which * 2, (int) cp, nps);
//            } catch (NumberFormatException e) {
//                return -5;
//            }
//        }
//
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
