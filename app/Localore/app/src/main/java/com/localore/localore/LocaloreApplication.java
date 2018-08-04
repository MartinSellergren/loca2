package com.localore.localore;

import android.app.Application;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Session;
import com.localore.localore.model.User;
import com.localore.localore.modelManipulation.SessionControl;

import java.util.List;

public class LocaloreApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AppDatabase db = AppDatabase.getInstance(this);
        List<User> users = db.userDao().loadAll();

        if (users.size() == 0) {
            User newUser = new User("DefaultUser");
            long userId = db.userDao().insert(newUser);
            SessionControl.login(userId, db);
        }
        else {
            SessionControl.login(users.get(0).getId(), db);
        }
    }
}
