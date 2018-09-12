package android.serialport;

import android.os.SystemClock;

import com.power.control.DeviceControl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class UHFDeviceControl {
    //kt系列
    public static final String POWER_MAIN = "/sys/class/misc/mtgpio/pin";
    //tt系列
    public static final String POWER_EXTERNAL = "/sys/class/misc/aw9523/gpio";
    //sk80 新添加
    public static final String POWER_EXTERNAL2 = "/sys/class/misc/aw9524/gpio";
    //新设备上电路径
    public static final String POWER_NEWMAIN = "/sys/bus/platform/drivers/mediatek-pinctrl/10005000.pinctrl/mt_gpio";

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
        MAIN_AND_EXPAND,
        /**
         * 新设备 sd55上电
         */
        NEW_MAIN,
        /**
         * 9524上电sk80使用
         */
        EXPAND2,
        /**
         * 主板和9524上电
         */
        MAIN_AND_EXPAND2


    }

    private BufferedWriter CtrlFile;

    private String poweron = "";
    private String poweroff = "";
    private String currentPath = "";

    public UHFDeviceControl() throws IOException {

    }

    public UHFDeviceControl(String path) throws IOException {
        File DeviceName = new File(path);
        CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));    //open file
        currentPath = path;
    }


    private int[] gpios;

    /**
     * 此方法可单独设置gpio
     *
     * @param gpio 设置gpio
     */
    public void setGpio(int gpio) {
        if (currentPath.equals(POWER_EXTERNAL) || currentPath.equals(POWER_EXTERNAL2)) {
            poweron = gpio + "on";
            poweroff = gpio + "off";
        } else {
            poweron = "-wmode " + gpio + " 0";//将GPIO99设置为GPIO模式
            poweron = "-wdir " + gpio + " 1";//将GPIO99设置为输出模式
            poweron = "-wdout " + gpio + " 1";
            poweroff = "-wdout " + gpio + " 0";
        }
    }

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
            case "NEW_MAIN":
                this.power_type = PowerType.NEW_MAIN;
                break;
            case "EXPAND2":
                this.power_type = PowerType.EXPAND2;
                break;
            case "MAIN_AND_EXPAND2":
                this.power_type = PowerType.MAIN_AND_EXPAND2;
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
        UHFDeviceControl deviceControl = new UHFDeviceControl(UHFDeviceControl.POWER_MAIN);
        deviceControl.setGpio(gpio);
        deviceControl.writeON();
        deviceControl.DeviceClose();
    }

    /**
     * 主板下电
     *
     * @param gpio
     * @throws IOException
     */
    public void MainPowerOff(int gpio) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(UHFDeviceControl.POWER_MAIN);
        deviceControl.setGpio(gpio);
        deviceControl.WriteOff();
        deviceControl.DeviceClose();
    }

    /**
     * 外部扩展上电
     *
     * @param gpio
     * @throws IOException
     */
    public void ExpandPowerOn(int gpio) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(UHFDeviceControl.POWER_EXTERNAL);
        deviceControl.setGpio(gpio);
        deviceControl.writeON();
        deviceControl.DeviceClose();
    }

    public void Expand2PowerOn(int gpio) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(UHFDeviceControl.POWER_EXTERNAL2);
        deviceControl.setGpio(gpio);
        deviceControl.writeON();
        deviceControl.DeviceClose();
    }

    /**
     * 外部扩展下电
     *
     * @param gpio
     * @throws IOException
     */
    public void ExpandPowerOff(int gpio) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(UHFDeviceControl.POWER_EXTERNAL);
        deviceControl.setGpio(gpio);
        deviceControl.WriteOff();
        deviceControl.DeviceClose();
    }

    public void Expand2PowerOff(int gpio) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(UHFDeviceControl.POWER_EXTERNAL2);
        deviceControl.setGpio(gpio);
        deviceControl.WriteOff();
        deviceControl.DeviceClose();
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
                for (int i = 0; i < gpios.length; i++) {
                    MainPowerOn(gpios[i]);
                    SystemClock.sleep(100);
                }
                break;
            case EXPAND:
                for (int i = 0; i < gpios.length; i++) {
                    ExpandPowerOn(gpios[i]);
                    SystemClock.sleep(100);
                }
                break;
            case MAIN_AND_EXPAND:
                MainPowerOn(gpios[0]);
                SystemClock.sleep(100);
                for (int i = 1; i < gpios.length; i++) {
                    ExpandPowerOn(gpios[i]);
                    SystemClock.sleep(100);
                }
                break;
            case NEW_MAIN:
                for (int i = 0; i < gpios.length; i++) {
                    newSetGpioOn(gpios[i]);
                    SystemClock.sleep(100);
                }
                break;
            case EXPAND2:
                for (int i = 0; i < gpios.length; i++) {
                    Expand2PowerOn(gpios[i]);
                    SystemClock.sleep(100);
                }
                break;
            case MAIN_AND_EXPAND2:
                MainPowerOn(gpios[0]);
                SystemClock.sleep(100);
                for (int i = 1; i < gpios.length; i++) {
                    Expand2PowerOn(gpios[i]);
                    SystemClock.sleep(100);
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
                for (int i = 0; i < gpios.length; i++) {
                    MainPowerOff(gpios[i]);
                    SystemClock.sleep(100);
                }
                break;
            case EXPAND:
                for (int i = 0; i < gpios.length; i++) {
                    ExpandPowerOff(gpios[i]);
                    SystemClock.sleep(100);
                }
                break;
            case EXPAND2:
                for (int i = 0; i < gpios.length; i++) {
                    Expand2PowerOff(gpios[i]);
                    SystemClock.sleep(100);
                }
                break;
            case MAIN_AND_EXPAND:
                MainPowerOff(gpios[0]);
                for (int i = 1; i < gpios.length; i++) {
                    ExpandPowerOff(gpios[i]);
                }
                break;
            case MAIN_AND_EXPAND2:
                MainPowerOff(gpios[0]);
                for (int i = 1; i < gpios.length; i++) {
                    Expand2PowerOff(gpios[i]);
                }
                break;
            case NEW_MAIN:
                for (int i = 0; i < gpios.length; i++) {
                    newSetGpioOff(gpios[i]);
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

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * 设置新设备上电 例如sd55 sd60
     *
     * @param gpio
     * @throws IOException
     */
    public void newSetGpioOn(int gpio) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(POWER_NEWMAIN);
        deviceControl.CtrlFile.write("out " + gpio + " 1");//将GPIO设置为高电平
        deviceControl.CtrlFile.flush();
        newSetMode(gpio);
        newSetDir(gpio, 1);

    }

    /**
     * 设置新设备下点 例如sd55 sd60
     *
     * @param gpio
     * @throws IOException
     */
    public void newSetGpioOff(int gpio) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(POWER_NEWMAIN);
        deviceControl.CtrlFile.write("out " + gpio + " 0");//将GPIO设置为低电平
        deviceControl.CtrlFile.flush();

    }

    /**
     * 设置gpio为 gpio模式 例如sd55 sd60
     *
     * @param gpio
     * @throws IOException
     */
    public void newSetMode(int gpio) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(POWER_NEWMAIN);
        deviceControl.CtrlFile.write("mode " + gpio + " 0");//将GPIO设置为GPIO模式
        deviceControl.CtrlFile.flush();

    }

    /**
     * 设置gpio为输入/输出模式 例如sd55 sd60
     *
     * @param gpio 要操作的gpio
     * @param dir  0：输入模式  1：输出模式
     * @throws IOException
     */
    public void newSetDir(int gpio, int dir) throws IOException {
        UHFDeviceControl deviceControl = new UHFDeviceControl(POWER_NEWMAIN);
        deviceControl.CtrlFile.write("dir " + gpio + " " + dir);//将GPIO99设置为输出模式
        deviceControl.CtrlFile.flush();

    }
}