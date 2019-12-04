package com.speedata.uhf;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by 张明_ on 2018/3/15.
 */

public class MyApp extends Application {
    /**
     * 单例
     */
    private static MyApp m_application;

    private IUHFService iuhfService;

    public static MyApp getInstance() {
        return m_application;
    }

    /**
     * 是否启动快速模式
     */
    public static boolean isFastMode = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("APP", "onCreate");
        m_application = this;
        Context context = getApplicationContext();
        // 获取当前包名
        String packageName = context.getPackageName();
        // 获取当前进程名
        String processName = getProcessName(android.os.Process.myPid());
        // 设置是否为上报进程
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        // 初始化Bugly
        Bugly.init(getApplicationContext(), "75242a29e5", true, strategy);

        setIuhfService();
//        startService(new Intent(this, MyService.class));
    }

    public void setIuhfService() {

        try {
            iuhfService = UHFManager.getUHFService(getApplicationContext());
            Log.d("UHFService", "iuhfService初始化: " + iuhfService);
            if (iuhfService != null) {
                int i = 0;
                i = iuhfService.setLowpowerScheduler(50, 0);
                Log.d("zzc:", "===isFirstInit===setReadTime:" + i);
                Log.d("zzc:", "===isFirstInit===setSleep:" + i);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    boolean cn = getApplicationContext().getResources().getConfiguration().locale.getCountry().equals("CN");
                    if (cn) {
                        Toast.makeText(getApplicationContext(), "模块不存在", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Module does not exist", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public IUHFService getIuhfService() {
        return iuhfService;
    }

    /**
     * 获取进程号对应的进程名
     *
     * @param pid 进程号
     * @return 进程名
     */
    private static String getProcessName(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

}
