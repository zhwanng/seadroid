package com.seafile.seadroid2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.seafile.seadroid2.framework.util.SLogs;
import com.seafile.seadroid2.framework.datastore.sp.Sorts;

import java.io.File;

public class App {
    public static void init() {

        //
        Sorts.init();


        //init slogs
        SLogs.init();


        SLogs.printAppEnvInfo();
    }
}
