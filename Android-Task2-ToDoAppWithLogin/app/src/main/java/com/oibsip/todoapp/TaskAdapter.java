package com.oibsip.todoapp;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {

    public interface Listener {
        void onToggle(Task task, boolean completed);
        void onDelete(Task task);
    }

    private final Listener listener;

    public TaskAdapter(Context context, List<Task> tasks, Listener listener) {
        super(context, 0, tasks);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_task, parent, false);
        }
        Task task = getItem(position);

        CheckBox cb = convertView.findViewById(R.id.cbDone);
        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvNotes = convertView.findViewById(R.id.tvNotes);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDelete);

        tvTitle.setText(task.title);
        if (task.notes == null || task.notes.isEmpty()) {
            tvNotes.setVisibility(View.GONE);
        } else {
            tvNotes.setVisibility(View.VISIBLE);
            tvNotes.setText(task.notes);
        }

        applyStrike(tvTitle, task.completed);
        tvTitle.setAlpha(task.completed ? 0.5f : 1f);

        // Detach listener before setting state to avoid recycled-view callbacks
        cb.setOnCheckedChangeListener(null);
        cb.setChecked(task.completed);
        cb.setOnCheckedChangeListener((b, checked) -> listener.onToggle(task, checked));
        btnDelete.setOnClickListener(v -> listener.onDelete(task));

        return convertView;
    }

    private void applyStrike(TextView tv, boolean strike) {
        if (strike) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }
}
