package com.seafile.seadroid2;

import android.app.Application;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.seafile.seadroid2.gesturelock.AppLockManager;
import com.seafile.seadroid2.framework.monitor.ActivityMonitor;
import com.seafile.seadroid2.framework.notification.base.NotificationUtils;
import com.seafile.seadroid2.framework.util.CrashHandler;

import java.io.File;


public class SeadroidApplication extends Application {
    private static Context context;
    private static SeadroidApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        //
        App.init();

        // set gesture lock if available
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);


        //
        NotificationUtils.initNotificationChannels(this);

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);

        //This feature can be extended
        registerActivityLifecycleCallbacks(new ActivityMonitor());
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        context = this;
    }

    public static Context getAppContext() {
        return context;
    }

    public static SeadroidApplication getInstance() {
        return instance;
    }

}
