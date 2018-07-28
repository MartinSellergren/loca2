package com.localore.localore.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * A user of the app.
 */
@Entity
public class User {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * True if User is the currently active user on the system.
     * @inv True in max 1 user.
     */
    private boolean selected;

//    private String uname;
//    private String pass;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
