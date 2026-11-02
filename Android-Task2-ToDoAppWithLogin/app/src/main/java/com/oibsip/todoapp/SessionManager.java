package com.oibsip.todoapp;

import android.content.Context;
import android.content.SharedPreferences;

/** Keeps the logged-in user's id/name so the session survives app restarts. */
public class SessionManager {
    private static final String PREFS = "todo_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void login(long userId, String name) {
        prefs.edit().putLong(KEY_USER_ID, userId).putString(KEY_USER_NAME, name).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getLong(KEY_USER_ID, -1) != -1;
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
