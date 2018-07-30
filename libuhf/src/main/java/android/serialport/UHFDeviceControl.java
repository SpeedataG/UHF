package android.serialport;

import android.os.SystemClock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class UHFDeviceControl {
    //kt系列
    public static final String POWER_MAIN = "/sys/class/misc/mtgpio/pin";
    //tt系列
    public static final String POWER_EXTERNAL = "/sys/class/misc/aw9523/gpio";

    /**
     * 上电类型
     */
    public enum PowerType {
        /**
         * 主板
         */
        MAIN,
        /**
         * 外部扩展
         */
        EXPAND,
        /**
         * 主板和外部扩展
         */
        MAIN_AND_EXPAND
    }

    private BufferedWriter CtrlFile;

    private String poweron = "";
    private String poweroff = "";
    private String currentPath = "";

    public UHFDeviceControl(String path) throws IOException {
        File DeviceName = new File(path);
        CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));    //open file
        currentPath = path;
    }

    /**
     * 此方法可单独设置gpio
     *
     * @param gpio 设置gpio
     */
    public void setGpio(int gpio) {
        if (currentPath.equals(POWER_EXTERNAL)) {
            poweron = gpio + "on";
            poweroff = gpio + "off";
        } else {
            poweron = "-wdout " + gpio + " 1";
            poweroff = "-wdout " + gpio + " 0";
        }
    }

    int[] gpios;
    private PowerType power_type;

    /**
     * @param power_type 上电类型
     * @param gpios      若为主板上电 gpio[0]需为主板gpio 扩展gpio可以有多个
     * @throws IOException
     */
    public UHFDeviceControl(PowerType power_type, int... gpios) throws IOException {
        this.gpios = gpios;
        this.power_type = power_type;
    }

    /**
     * @param power_type 上电类型
     * @param gpios      若为主板上电 gpio[0]需为主板gpio 扩展gpio可以有多个
     * @throws IOException
     */
    public UHFDeviceControl(String power_type, int... gpios) throws IOException {
        this.gpios = gpios;
        switch (power_type) {
            case "MAIN":
                this.power_type = PowerType.MAIN;
                break;
            case "EXPAND":
                this.power_type = PowerType.EXPAND;
                break;
            case "MAIN_AND_EXPAND":
                this.power_type = PowerType.MAIN_AND_EXPAND;
                break;
        }
    }

    /**
     * 主板上电
     *
     * @param gpio
     * @throws IOException
     */
    public void MainPowerOn(int gpio) throws IOException {
        UHFDeviceControl UHFDeviceControl = new UHFDeviceControl(POWER_MAIN);
        UHFDeviceControl.setGpio(gpio);
        UHFDeviceControl.writeON();
        UHFDeviceControl.DeviceClose();
    }

    /**
     * 主板下电
     *
     * @param gpio
     * @throws IOException
     */
    public void MainPowerOff(int gpio) throws IOException {
        UHFDeviceControl UHFDeviceControl = new UHFDeviceControl(POWER_MAIN);
        UHFDeviceControl.setGpio(gpio);
        UHFDeviceControl.WriteOff();
        UHFDeviceControl.DeviceClose();
    }

    /**
     * 外部扩展上电
     *
     * @param gpio
     * @throws IOException
     */
    public void ExpandPowerOn(int gpio) throws IOException {
        UHFDeviceControl UHFDeviceControl = new UHFDeviceControl(POWER_EXTERNAL);
        UHFDeviceControl.setGpio(gpio);
        UHFDeviceControl.writeON();
        UHFDeviceControl.DeviceClose();
    }

    /**
     * 外部扩展下电
     *
     * @param gpio
     * @throws IOException
     */
    public void ExpandPowerOff(int gpio) throws IOException {
        UHFDeviceControl UHFDeviceControl = new UHFDeviceControl(POWER_EXTERNAL);
        UHFDeviceControl.setGpio(gpio);
        UHFDeviceControl.WriteOff();
        UHFDeviceControl.DeviceClose();
    }


    /**
     * 写ON
     *
     * @throws IOException
     */
    private void writeON() throws IOException {
        CtrlFile.write(poweron);
        CtrlFile.flush();
    }

    /**
     * 写off
     *
     * @throws IOException
     */
    private void WriteOff() throws IOException {
        CtrlFile.write(poweroff);
        CtrlFile.flush();
    }

    /**
     * 构造函数之后 可带调用此方法上电
     *
     * @throws IOException 抛出异常
     */
    public void PowerOnDevice() throws IOException        //poweron id device
    {
        switch (power_type) {
            case MAIN:
//                MainPowerOn(gpios[0]);
//                SystemClock.sleep(200);
                for (int i = 0; i < gpios.length; i++) {
                    MainPowerOn(gpios[i]);
                    SystemClock.sleep(200);
                }
                break;
            case EXPAND:
                ExpandPowerOn(gpios[0]);
                SystemClock.sleep(200);
                break;
            case MAIN_AND_EXPAND:
                MainPowerOn(gpios[0]);
                SystemClock.sleep(200);
                for (int i = 1; i < gpios.length; i++) {
                    ExpandPowerOn(gpios[i]);
                    SystemClock.sleep(200);
                }
                break;
            default:
                break;
        }
    }


    /**
     * 构造函数后 程序退出时可调用此方法下电
     *
     * @throws IOException 抛出异常
     */
    public void PowerOffDevice() throws IOException        //poweroff id device
    {
        switch (power_type) {
            case MAIN:
                MainPowerOff(gpios[0]);
                break;
            case EXPAND:
                ExpandPowerOff(gpios[0]);
                break;
            case MAIN_AND_EXPAND:
                MainPowerOff(gpios[0]);
                for (int i = 1; i < gpios.length; i++) {
                    ExpandPowerOff(gpios[i]);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 关闭文件
     *
     * @throws IOException
     */
    public void DeviceClose() throws IOException        //close file
    {
        CtrlFile.close();
    }

    /**
     * 设置制定管脚模式  0为gpio模式
     *
     * @param num  管脚
     * @param mode 0为gpio模式
     * @throws IOException
     */
    public void setMode(int num, int mode) throws IOException {
        CtrlFile.write("-wmode" + num + " " + mode);   //设置为模式 0为GPIO模式
        CtrlFile.flush();
    }

    /**
     * 设置输入输出模式
     *
     * @param num  管脚
     * @param mode 输入输出模式
     * @param path 路径
     * @throws IOException
     */
    public void setDir(int num, int mode, String path) throws IOException {
        File DeviceName = new File(path);
        CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));    //open file
        CtrlFile.write("-wdir" + num + " " + mode);   //设置为输入输出
        CtrlFile.flush();
    }

    /**
     * 设置上拉下拉
     *
     * @param num
     * @param mode
     * @param path
     * @throws IOException
     */
    public void setPull(int num, int mode, String path) throws IOException {
        File DeviceName = new File(path);
        CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));    //open file
        CtrlFile.write("-wpsel" + num + " " + mode);   //设置为输入输出
        CtrlFile.flush();
        CtrlFile.write("-wpen" + num + " " + mode);   //设置为输入输出
        CtrlFile.flush();
    }
}