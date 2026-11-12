package com.oibsip.stopwatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Stopwatch based on SystemClock.elapsedRealtime().
 *
 * Elapsed time = accumulatedMs + (now - startedAt) while running. Because it is computed from a
 * monotonic clock rather than by counting Handler ticks, the time stays correct when the user
 * leaves the app (onPause) and comes back (onResume), and across screen rotation.
 */
public class MainActivity extends AppCompatActivity {

    private static final long TICK_MS = 10;

    private static final String KEY_RUNNING = "running";
    private static final String KEY_ACCUMULATED = "accumulated";
    private static final String KEY_STARTED_AT = "startedAt";
    private static final String KEY_LAST_LAP = "lastLapMs";
    private static final String KEY_LAPS = "laps";

    private TextView tvTime, tvMillis, tvNoLaps;
    private Button btnStart, btnStop, btnLap, btnReset;
    private ListView listLaps;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<String> laps = new ArrayList<>(); // newest first
    private ArrayAdapter<String> lapAdapter;

    private boolean running = false;
    private long accumulatedMs = 0;   // time banked from previous runs
    private long startedAt = 0;       // elapsedRealtime when the current run began
    private long lastLapMs = 0;       // elapsed time at the previous lap

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateDisplay();
            if (running) {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTime = findViewById(R.id.tvTime);
        tvMillis = findViewById(R.id.tvMillis);
        tvNoLaps = findViewById(R.id.tvNoLaps);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnLap = findViewById(R.id.btnLap);
        btnReset = findViewById(R.id.btnReset);
        listLaps = findViewById(R.id.listLaps);

        lapAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, laps);
        listLaps.setAdapter(lapAdapter);

        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean(KEY_RUNNING);
            accumulatedMs = savedInstanceState.getLong(KEY_ACCUMULATED);
            startedAt = savedInstanceState.getLong(KEY_STARTED_AT);
            lastLapMs = savedInstanceState.getLong(KEY_LAST_LAP);
            ArrayList<String> saved = savedInstanceState.getStringArrayList(KEY_LAPS);
            if (saved != null) laps.addAll(saved);
        }

        btnStart.setOnClickListener(v -> start());
        btnStop.setOnClickListener(v -> stop());
        btnLap.setOnClickListener(v -> lap());
        btnReset.setOnClickListener(v -> reset());

        updateButtons();
        updateLapsVisibility();
        updateDisplay();
    }

    // ---------- Actions ----------

    private void start() {
        if (running) return; // guards against rapid double taps
        running = true;
        startedAt = SystemClock.elapsedRealtime();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        updateButtons();
    }

    private void stop() {
        if (!running) return;
        accumulatedMs += SystemClock.elapsedRealtime() - startedAt;
        running = false;
        handler.removeCallbacks(ticker);
        updateDisplay();
        updateButtons();
    }

    private void lap() {
        if (!running) return;
        long total = getElapsedMs();
        long split = total - lastLapMs;
        lastLapMs = total;
        laps.add(0, String.format(Locale.US, "Lap %d     %s     (+%s)",
                laps.size() + 1, format(total), format(split)));
        lapAdapter.notifyDataSetChanged();
        updateLapsVisibility();
    }

    private void reset() {
        running = false;
        handler.removeCallbacks(ticker);
        accumulatedMs = 0;
        startedAt = 0;
        lastLapMs = 0;
        laps.clear();
        lapAdapter.notifyDataSetChanged();
        updateLapsVisibility();
        updateDisplay();
        updateButtons();
    }

    // ---------- Lifecycle ----------

    @Override
    protected void onResume() {
        super.onResume();
        // Elapsed time is derived from the clock, so just resume refreshing the screen.
        updateDisplay();
        if (running) {
            handler.removeCallbacks(ticker);
            handler.post(ticker);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop UI updates while hidden; the timer itself keeps counting via elapsedRealtime.
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_RUNNING, running);
        outState.putLong(KEY_ACCUMULATED, accumulatedMs);
        outState.putLong(KEY_STARTED_AT, startedAt);
        outState.putLong(KEY_LAST_LAP, lastLapMs);
        outState.putStringArrayList(KEY_LAPS, laps);
    }

    // ---------- UI helpers ----------

    private long getElapsedMs() {
        return running
                ? accumulatedMs + (SystemClock.elapsedRealtime() - startedAt)
                : accumulatedMs;
    }

    private void updateDisplay() {
        long ms = getElapsedMs();
        long totalSeconds = ms / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        tvTime.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));
        tvMillis.setText(String.format(Locale.US, ".%02d", (ms % 1000) / 10));
    }

    private void updateButtons() {
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
        btnLap.setEnabled(running);
        btnReset.setEnabled(running || accumulatedMs > 0);
        btnStart.setText(!running && accumulatedMs > 0 ? "Resume" : getString(R.string.start));
        btnStart.setAlpha(btnStart.isEnabled() ? 1f : 0.4f);
        btnStop.setAlpha(btnStop.isEnabled() ? 1f : 0.4f);
        btnLap.setAlpha(btnLap.isEnabled() ? 1f : 0.4f);
        btnReset.setAlpha(btnReset.isEnabled() ? 1f : 0.4f);
    }

    private void updateLapsVisibility() {
        tvNoLaps.setVisibility(laps.isEmpty() ? TextView.VISIBLE : TextView.GONE);
    }

    private static String format(long ms) {
        long totalSeconds = ms / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        long cs = (ms % 1000) / 10;
        return h > 0
                ? String.format(Locale.US, "%02d:%02d:%02d.%02d", h, m, s, cs)
                : String.format(Locale.US, "%02d:%02d.%02d", m, s, cs);
    }
}
