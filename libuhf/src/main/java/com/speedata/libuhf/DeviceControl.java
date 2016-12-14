package com.speedata.libuhf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DeviceControl
{
	private String dev = null;
	private int gpio = -1;
	
	DeviceControl(String path, int p) { dev = path; gpio = p; }
	
	public int PowerOnDevice()
	{
		try {
			File DeviceName = new File(dev);
			BufferedWriter CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));
			CtrlFile.write("-wdout" + gpio + " 1");
			//CtrlFile.write("-wdout64 1");
			CtrlFile.flush();
			CtrlFile.close();
		} catch (IOException e) {
			return -1;
		}
		return 0;
	}
	
	public int PowerOffDevice()
	{
  		try {
			File DeviceName = new File(dev);
			BufferedWriter CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));
			CtrlFile.write("-wdout" + gpio + " 0");
			//CtrlFile.write("-wdout64 0");
			CtrlFile.flush();
			CtrlFile.close();
		} catch (IOException e) {
			return -1;
		}
		return 0;
	}
}