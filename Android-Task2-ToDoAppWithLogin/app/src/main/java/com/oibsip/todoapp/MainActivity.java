package com.oibsip.todoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.Listener {

    private DatabaseHelper db;
    private SessionManager session;
    private final List<Task> tasks = new ArrayList<>();
    private TaskAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        db = new DatabaseHelper(this);

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText("Hi, " + session.getUserName() + "!");
        tvEmpty = findViewById(R.id.tvEmpty);
        ListView listView = findViewById(R.id.listTasks);
        adapter = new TaskAdapter(this, tasks, this);
        listView.setAdapter(adapter);
        listView.setEmptyView(tvEmpty);

        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logout());
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddDialog());

        refresh();
    }

    private void refresh() {
        tasks.clear();
        tasks.addAll(db.getTasks(session.getUserId()));
        adapter.notifyDataSetChanged();
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText etTitle = view.findViewById(R.id.etTaskName);
        EditText etNotes = view.findViewById(R.id.etTaskNotes);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_task)
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        // Override the positive click so the dialog stays open when validation fails
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        etTitle.setError("Task name is required");
                        return;
                    }
                    db.addTask(session.getUserId(), title, etNotes.getText().toString().trim());
                    refresh();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    @Override
    public void onToggle(Task task, boolean completed) {
        db.setCompleted(session.getUserId(), task.id, completed);
        refresh();
    }

    @Override
    public void onDelete(Task task) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage("Delete \"" + task.title + "\" permanently?")
                .setPositiveButton(R.string.delete, (d, w) -> {
                    db.deleteTask(session.getUserId(), task.id);
                    refresh();
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void logout() {
        session.logout();
        goToLogin();
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
