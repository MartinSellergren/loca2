package com.localore.localore;

import android.app.Application;

public class LocaloreApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // For now!
        AppDatabase.getInstance(this).clearAllTables();
    }
}
