package com.localore.localore;

import android.app.Application;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Session;
import com.localore.localore.model.User;

public class LocaloreApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //AppDatabase.getInstance(this).clearAllTables();
    }
}
