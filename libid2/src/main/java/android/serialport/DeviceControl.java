package android.serialport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DeviceControl {
    public static final String POWER_MAIN = "/sys/class/misc/mtgpio/pin";
    public static final String POWER_EXTERNAL = "/sys/class/misc/aw9523/gpio";

    public enum PowerType {
        MAIN, EXPAND, MAIN_AND_EXPAND
    }

    private BufferedWriter CtrlFile;

    private String poweron = "";
    private String poweroff = "";
//    private int Gpio;
    private String currentPath = "";

    public void setGpio(int gpio) {
//        Gpio = gpio;
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

    public DeviceControl(PowerType power_type, int... gpios) throws IOException {
        this.gpios = gpios;
        this.power_type = power_type;
    }

    private void MainPowerOn(int gpio) throws IOException {
        DeviceControl deviceControl = new DeviceControl(DeviceControl.POWER_MAIN);
        deviceControl.setGpio(gpio);
        deviceControl.writeON();
        deviceControl.DeviceClose();
    }

    private void MainPowerOff(int gpio) throws IOException {
        DeviceControl deviceControl = new DeviceControl(DeviceControl.POWER_MAIN);
        deviceControl.setGpio(gpio);
        deviceControl.WriteOff();
        deviceControl.DeviceClose();
    }

    private void ExpandPowerOn(int gpio) throws IOException {
        DeviceControl deviceControl = new DeviceControl(DeviceControl.POWER_EXTERNAL);
        deviceControl.setGpio(gpio);
        deviceControl.writeON();
        deviceControl.DeviceClose();
    }

    private void ExpandPowerOff(int gpio) throws IOException {
        DeviceControl deviceControl = new DeviceControl(DeviceControl.POWER_EXTERNAL);
        deviceControl.setGpio(gpio);
        deviceControl.WriteOff();
        deviceControl.DeviceClose();
    }


    private DeviceControl(String path) throws IOException {
        File DeviceName = new File(path);
        CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));    //open file
        currentPath = path;
    }

    private void writeON() throws IOException {
        CtrlFile.write(poweron);
        CtrlFile.flush();
    }

    private void WriteOff() throws IOException {
        CtrlFile.write(poweroff);
        CtrlFile.flush();
    }

    public void PowerOnDevice() throws IOException        //poweron id device
    {
        switch (power_type) {
            case MAIN:
                MainPowerOn(gpios[0]);
                break;
            case EXPAND:
                ExpandPowerOn(gpios[0]);
                break;
            case MAIN_AND_EXPAND:
                MainPowerOn(gpios[0]);
                ExpandPowerOn(gpios[1]);
                break;
            default:
                break;
        }
    }


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
                ExpandPowerOff(gpios[1]);
                break;
            default:
                break;
        }
    }

//    public void resetGPIO()
    public void DeviceClose() throws IOException        //close file
    {
        CtrlFile.close();
    }
}