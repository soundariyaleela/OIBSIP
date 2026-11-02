package com.oibsip.todoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "todo.db";
    private static final int DB_VERSION = 1;

    private static final String T_USERS = "users";
    private static final String T_TASKS = "tasks";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "email TEXT NOT NULL UNIQUE,"
                + "salt TEXT NOT NULL,"
                + "password_hash TEXT NOT NULL)");
        db.execSQL("CREATE TABLE " + T_TASKS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL,"
                + "title TEXT NOT NULL,"
                + "notes TEXT,"
                + "completed INTEGER NOT NULL DEFAULT 0,"
                + "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        onCreate(db);
    }

    // ---------- Password hashing (salted SHA-256, no plain text stored) ----------

    private static String newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    private static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- Users ----------

    /** @return new user id, or -1 if the email is already registered. */
    public long registerUser(String name, String email, String password) {
        String salt = newSalt();
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("email", email.toLowerCase().trim());
        v.put("salt", salt);
        v.put("password_hash", hash(password, salt));
        return getWritableDatabase().insert(T_USERS, null, v); // -1 on UNIQUE violation
    }

    /** @return {id, name} on success, or null if credentials are wrong. */
    public String[] authenticate(String email, String password) {
        try (Cursor c = getReadableDatabase().query(T_USERS,
                new String[]{"id", "name", "salt", "password_hash"},
                "email = ?", new String[]{email.toLowerCase().trim()}, null, null, null)) {
            if (c.moveToFirst()) {
                String salt = c.getString(2);
                String stored = c.getString(3);
                if (MessageDigest.isEqual(
                        stored.getBytes(StandardCharsets.UTF_8),
                        hash(password, salt).getBytes(StandardCharsets.UTF_8))) {
                    return new String[]{String.valueOf(c.getLong(0)), c.getString(1)};
                }
            }
        }
        return null;
    }

    // ---------- Tasks (always scoped to a user) ----------

    public long addTask(long userId, String title, String notes) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("title", title);
        v.put("notes", notes);
        v.put("completed", 0);
        return getWritableDatabase().insert(T_TASKS, null, v);
    }

    public List<Task> getTasks(long userId) {
        List<Task> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(T_TASKS,
                new String[]{"id", "title", "notes", "completed"},
                "user_id = ?", new String[]{String.valueOf(userId)},
                null, null, "completed ASC, id DESC")) {
            while (c.moveToNext()) {
                list.add(new Task(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3) == 1));
            }
        }
        return list;
    }

    public void setCompleted(long userId, long taskId, boolean completed) {
        ContentValues v = new ContentValues();
        v.put("completed", completed ? 1 : 0);
        getWritableDatabase().update(T_TASKS, v, "id = ? AND user_id = ?",
                new String[]{String.valueOf(taskId), String.valueOf(userId)});
    }

    public void deleteTask(long userId, long taskId) {
        getWritableDatabase().delete(T_TASKS, "id = ? AND user_id = ?",
                new String[]{String.valueOf(taskId), String.valueOf(userId)});
    }
}
