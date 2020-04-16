package com.fitpolo.demo.h707;

import android.app.Application;

import com.fitpolo.support.MokoSupport;

import es.dmoral.toasty.Toasty;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description
 */

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Toasty.Config.getInstance().apply();
        // 初始化
        MokoSupport.getInstance().init(getApplicationContext());
    }
}
