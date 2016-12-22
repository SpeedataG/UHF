/**
 * Created by ${石磊} on 2016/12/14. TO
 */

package com.speedata.libuhf;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.uhf_sdk.linkage.Linkage;
import com.uhf_sdk.model.Inventory_Data;
import com.uhf_sdk.model.Value_c;
import com.uhf_sdk.uhfpower.UhfPowaer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;





/**
 * Created by ${石磊} on 2016/12/14. TO
 */

public class FLX_QiLian implements IUHFService {

	private static String TAG = "FLX_QiLian";
	private Handler mHandler = null;
	private boolean isChecking = false;

	private static final String SERIALPORT = "/dev/ttyMT2";
	private static final String POWERCTL = "/sys/class/misc/mtgpio/pin";
	private static final int PW_GPIO = 94;
	private static UhfPowaer sUhfPowaer = new UhfPowaer(POWERCTL, PW_GPIO);

	//全局的串口句柄，底层通过句柄操作模块
	public static int open_Com = 0;

	private inventory_command_thread mInventoryCommandThread = null;
	private get_inventoryData_thread mGetInventoryDataThread;

	private Timer timer = new Timer(true);
	private MyTimerTask task;


	private class inventory_command_thread extends Thread {
		public void run() {
			super.run();
			Linkage.inventory_Start(open_Com);
		}
	}

	private class get_inventoryData_thread extends Thread {
		@Override
		public void run() {
			super.run();
			while (isChecking) {
				Message msg = new Message();
				ArrayList<Tag_Data> tg = get_inventory_data();
				if (tg != null) {
					msg.what = 1;
					msg.obj = tg;
					mHandler.sendMessage(msg);
				}
			}

		}
	}


	/**
	 * open_com 当其值大于0，则打开串口成功，否则失败
	 */
	@Override
	public int OpenDev() {
		try {
			//初始化模块，为模块上电，为保证上电成功，建议先下电，在上电
			sUhfPowaer.PowerOffDevice();
			sUhfPowaer.PowerOnDevice();
			Thread.sleep(400);
			open_Com = Linkage.open_Serial(SERIALPORT);
			if (open_Com > 0) {
				Log.i(TAG, "-----open_Com-----" + open_Com);
				return 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return -1;
	}

	@Override
	public void CloseDev() {

		Linkage.close_Serial(open_Com);
		sUhfPowaer.PowerOffDevice();

	}
	/**
	 * 下发盘点开始命令，下发后模块循环向外发送盘点信号 注意：本模块定义的盘点模式为：循环发命令65535次后，模块自动停止盘点。 所以要达到长时间不间断盘点，应设置 一个定时器，在间隔约10分钟时，重新发送一次盘点开始命令。
	 */
	@Override
	public void inventory_start() {

		if (isChecking) {
			return;
		}
		isChecking = true;
		mInventoryCommandThread = new inventory_command_thread();
		mInventoryCommandThread.start();
		mGetInventoryDataThread = new get_inventoryData_thread();
		mGetInventoryDataThread.start();

		/**
		 * 定时器，在间隔约10分钟时重新下发盘点开始命令
		 */
		if (timer != null) {
			if (task != null) {
				task.cancel(); // 将原任务从队列中移除
			}
			task = new MyTimerTask(); // 新建一个任务
			timer.schedule(task, 10 * 60 * 1000, 10 * 60 * 1000);
		}

	}

	/**
	 * 该方法必定是在inventory_start()调用后，才能调用，调用该方法会开启一个死循环，无限接收模块返回的数据，并处理
	 */
	@Override
	public void inventory_start(Handler hd) {

		reg_handler(hd);
		inventory_start();


	}

	@Override
	public void inventory_stop() {
		if (!isChecking) {
			return;
		}
		isChecking = false;
		mGetInventoryDataThread.interrupt();
		mInventoryCommandThread.interrupt();
		Linkage.inventory_Stop(open_Com);

	}



	private ArrayList<Tag_Data> get_inventory_data() {
		ArrayList<Tag_Data> tagData = new ArrayList<>();
		Inventory_Data[] stInvData = new Inventory_Data[512];
		int num = Linkage.inventory_Data(open_Com, stInvData);
		if (num > 0) {
			for (int i = 0; i < num; i++) {
				if (stInvData[i].getEPC_len() > 0 && stInvData[i].getEPC_len() < 80) {
					byte[] n_epc_result =new byte[stInvData[i].getEPC_len()];
					byte[] n_epc = stInvData[i].getEPC_Data();
					for (int j = 0; j < n_epc_result.length; j++) {
						n_epc_result[j]=n_epc[j];
					}
					tagData.add(new Tag_Data(null,n_epc_result));
				}
			}
			return tagData;
		}
		return null;
	}


	/*
	 * 读标签函数
	 * 参数 area：1:为EPC区域。2：为TID区域。3：为USER区域。
	 * 从标签 area 区的 addr 位置（以 word 计算）读取 count 个值（以 word 计算）
   * passwd 是访问密码，如果区域没被锁就给 0 值。
	 */
	@Override
	public byte[] read_area(int area, int addr, int count, int passwd) {
		Value_c result = new Value_c();

		if ((area > 4) || (area < 0)) {
			return null;
		}

		char[] ReadText;
		char[] access_password = new char[4];

		access_password[0] = (char) ((passwd / 0x1000000) % 0x100);
		access_password[1] = (char) ((passwd / 0x10000) % 0x100);
		access_password[2] = (char) ((passwd / 0x100) % 0x100);
		access_password[3] = (char) ((passwd) % 0x100);

		ReadText = Linkage.read_Label(open_Com, area, addr, count/2, access_password,
				result);

		if ((ReadText != null) && (result.value == 0)) {
			Log.i(TAG, "--------ReadText 原始--------" + Linkage.c2hexs(ReadText, ReadText.length));
			return charToByte(ReadText, ReadText.length);
		}
		return null;
	}


	// byte转char

	private static char[] byteToChar(byte[] data, int length) {
		if (length % 2 == 0) {
			char[] chars = new char[length / 2];
			char a;
			for (int i = 0; i < length/2; ++i) {
				a= (char) (data[i*2]*0x100+data[i*2+1]);
				chars[i] =a;
			}
			return chars;
		}
		return null;
	}


	public static byte[] charToByte(char[] data, int length) {
		byte[] bytes = new byte[2 * length];

		for (int i = 0; i < length; ++i) {
			bytes[2 * i] = (byte) (data[i] >> 8);
			bytes[2 * i + 1] = (byte) data[i];
		}
		return bytes;
	}

	/*
	 * 写入标签函数
	 * 1:为EPC区域。2：为TID区域。3：为USER区域。
	 * 从标签 area 区的 addr 位置（以 word 计算）写入count 个值（以 word 计算）
   * passwd 是访问密码，如果区域没被锁就给 0 值。
	 */
	@Override
	public int write_area(int area, int addr, int passwd, byte[] content) {

		int result = 0;

		if ((area > 0) && (area <= 3)) {
			char[] access_password = new char[4];

			access_password[0] = (char) ((passwd / 0x1000000) % 0x100);
			access_password[1] = (char) ((passwd / 0x10000) % 0x100);
			access_password[2] = (char) ((passwd / 0x100) % 0x100);
			access_password[3] = (char) ((passwd) % 0x100);

			result = Linkage.write_Label(open_Com, area, addr, (content.length / 2), byteToChar(content,content.length),
					access_password);
		}
		return result;
	}

	/**
	 * 启用掩码：返回值0：为成功，其他均为失败 注意：1、当需要启用掩码时，注意当掩码长度为0时，掩码启用无效的，相当于取消了掩码 2、设置掩码（掩码长度不为0的）成功后，在不退出程序，不执行第一条的情况下，该掩码一直有效。
	 * 3、取消掩码：应设置掩码长度为0，并在启用掩码状态读取一次，本模块无取消掩码函数
	 */
	@Override
	public int select_card(byte[] epc) {

		int epc_area = 1;//1:为EPC区域。2：为TID区域。3：为USER区域。
		int epc_lenght = epc.length/2;
		int epc_add = 2;
		int mask_sel = 0;//注意：该参数必须和模块中参数的sel值保持一致（当前为模块初始值）
		int mask_session = 0;//注意：该参数必须和模块中参数的session值保持一致（当前为模块初始值）

		if (epc.length > 0) {
			return Linkage.set_Mask(open_Com, mask_sel, mask_session, epc_area, epc_add, epc_lenght,
					byteToChar(epc,epc.length));
		}
		return -1;
	}

	/**
	 * @param power 取值为19到30之间,小于19的值设置成功后，功率依然是19，大于30的功率设置成功后，功率依然是30 power最大值30，最小值19
	 * @return 返回0为成功，其他均为失败
	 */
	@Override
	public int set_antenna_power(int power) {
		int iu32 = power * 100;
		return Linkage.set_Power(open_Com, iu32);
	}

	/**
	 * @return value_c.value 值为0成功，其他均为失败
	 */
	@Override
	public int get_antenna_power() {
		Value_c value_c = new Value_c();
		int power_value = Linkage.get_Power(open_Com, value_c);
		if (value_c.value == 0) {
			return power_value / 100;
		} else {
			return -1;

		}
	}

	public INV_TIME get_inventory_time() {
		Single_Inventory_Time_Config t = new Single_Inventory_Time_Config();
		return new INV_TIME(t.iWorkTime, t.iRestTime);
	}

	public int set_inventory_time(int work_t, int rest_t) {
		Single_Inventory_Time_Config t = new Single_Inventory_Time_Config(work_t, rest_t);
		return 0;
	}

	public int MakeSetValid() {
		return 0;
	}

	@Override
	public int get_inventory_mode() {
		return -1;
	}

	@Override
	public int set_inventory_mode(int m) {
		return -1;
	}


	/**
	 * @param region 为region=1:中国920_925(0),region=0：中国840_845(4),region=2欧规;
	 * @return 值为0成功，其他均为失败
	 */
	@Override
	public int set_freq_region(int region) {

		Linkage.RFID_18K6C_COUNTRY_REGION region_ql = null;
		switch (region) {
			case REGION_CHINA_840_845:
				region_ql = Linkage.RFID_18K6C_COUNTRY_REGION.China840_845;
				break;
			case REGION_CHINA_920_925:
				region_ql = Linkage.RFID_18K6C_COUNTRY_REGION.China920_925;
				break;
			case REGION_CHINA_902_928:
				return -1;
			case REGION_EURO_865_868:
				region_ql = Linkage.RFID_18K6C_COUNTRY_REGION.Europe_Area;
				break;
		}

		return Linkage.set_Region(open_Com, region_ql);
	}

	//没有获取频率区域的函数？
	@Override public int get_freq_region() {
		return 0;
	}

	@Override public void reg_handler(Handler hd) {
		mHandler = hd;
	}
	// 0：开放
	// 1：永久开放
	// 2：锁定
	// 3：永久锁定
	private static final int KILL_PASSWORD_AREA = 0;
	private static final int ACCESS_PASSWORD_AREA = 1;
	private static final int EPC_AREA = 2;
	private static final int TID_AREA = 3;
	private static final int USER_AREA = 4;

	//锁定标签
	@Override
	public int setlock(int type, int area, int passwd) {

		int lock_statue = 0;
		if (type < 0 || type > 3 || area < 0 || area > 4) {
			return -1;
		}
		char[] access_password = new char[4];

		access_password[0] = (char) ((passwd / 0x1000000) % 0x100);
		access_password[1] = (char) ((passwd / 0x10000) % 0x100);
		access_password[2] = (char) ((passwd / 0x100) % 0x100);
		access_password[3] = (char) ((passwd) % 0x100);

		/**
		 * 锁定标签函数的参数：从第一个参数起依次是：
		 * 串口句柄，访问密码，销毁密码是否锁定（0不设置，1设置），销毁密码区锁定级别，访问密码是否锁定0不设置，1设置），访问密码区锁定级别
		 * ，epc区是否锁定（0不设置，1设置），eps	区锁定级别，tid密码是否锁定（0不设置，1设置），tid区锁定级别，
		 * user是否锁定（0不设置，1设置），user区锁定级别。
		 * lock_Label(int hReader, char[] pwd, int kill_mask, int kill_action, int access_mask,
		 *int access_action, int EPC_mask, int EPC_action, int TID_mask, int TID_action,
		 *int USER_mask, int USER_action);
		 *
		 */

		switch (area) {
			case KILL_PASSWORD_AREA:
				lock_statue = Linkage.lock_Label(open_Com, access_password, 1,
						type, 0, 0, 0, 0, 0, 0, 0, 0);
				break;
			case ACCESS_PASSWORD_AREA:
				lock_statue = Linkage.lock_Label(open_Com, access_password, 0,
						0, 1, type, 0, 0, 0, 0, 0, 0);
				break;
			case EPC_AREA:
				lock_statue = Linkage.lock_Label(open_Com, access_password, 0,
						0, 0, 0, 1, type, 0, 0, 0, 0);
				break;
			case TID_AREA:
				lock_statue = Linkage.lock_Label(open_Com, access_password, 0,
						0, 0, 0, 0, 0, 1, type, 0, 0);
				break;
			case USER_AREA:
				lock_statue = Linkage.lock_Label(open_Com, access_password, 0,
						0, 0, 0, 0, 0, 0, 0, 1, type);
				break;
		}

		if (lock_statue != 0) {
			return -1;
		}

		return lock_statue;
	}


	//销毁标签
	public int setkill(int ap, int kp) {

		char[] kill_password = new char[4];

		kill_password[0] = (char) ((kp / 0x1000000) % 0x100);
		kill_password[1] = (char) ((kp / 0x10000) % 0x100);
		kill_password[2] = (char) ((kp / 0x100) % 0x100);
		kill_password[3] = (char) ((kp) % 0x100);

		//销毁标签：本函数只需要将要被销毁的标签的销毁密码
		int kill_status = Linkage.kill_Label(open_Com, kill_password);

		if (kill_status != 0) {
			return -1;
		}
		return kill_status;
	}
	/**
	 * 定时器任务：在指定的时间之后，重新下发盘点指令
	 */
	private class MyTimerTask extends TimerTask {

		@Override
		public void run() {
			Linkage.inventory_Start(open_Com);
		}

	}
}

