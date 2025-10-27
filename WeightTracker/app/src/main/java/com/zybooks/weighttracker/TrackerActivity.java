package com.zybooks.weighttracker;

import android.content.SharedPreferences;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

// Screen that shows the weight list, lets the user add entries, and tracks goal status.
// Also handles sending an SMS alert when the user reaches their goal (if permission is granted).
public class TrackerActivity extends AppCompatActivity {

    // database + current user
    private DatabaseHelper db;
    private long userId;

    // goal banner (two lines)
    private TextView tvGoalWeightLine;
    private TextView tvGoalStatusLine;

    // list container (used across methods)
    private LinearLayout listContainer;

    // sms permission + demo number
    private static final int REQ_SMS = 2001;
    private static final String ALERT_NUMBER = "5551234567";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        // open db and read session user id
        db = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);
        if (userId <= 0) { finish(); return; } // guard: no session → exit to avoid null state

        // connect views
        tvGoalWeightLine = findViewById(R.id.tvGoalWeightLine);
        tvGoalStatusLine = findViewById(R.id.tvGoalStatusLine);
        ScrollView scrollGrid = findViewById(R.id.scrollGrid);

        // these can be local (only used in onCreate/listener)
        final EditText etWeight = findViewById(R.id.etWeight);
        final EditText etDate   = findViewById(R.id.etDate);
        Button btnAddEntry      = findViewById(R.id.btnAddEntry);

        // list container is the first child of the ScrollView
        listContainer = (LinearLayout) scrollGrid.getChildAt(0);

        // allow decimal weight input (prevents integer-only keyboards)
        etWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // add button: validate -> insert -> refresh UI -> maybe trigger SMS if goal reached
        btnAddEntry.setOnClickListener(v -> {
            String wStr = etWeight.getText().toString().trim();
            String dStr = etDate.getText().toString().trim();
            if (wStr.isEmpty() || dStr.isEmpty()) {
                toast(getString(R.string.enter_weight_date));
                return;
            }
            try {
                double lbs = Double.parseDouble(wStr);
                db.addWeight(userId, dStr, lbs);   // save entry
                etWeight.setText("");              // clear fields
                etDate.setText("");
                renderList();                      // rebuild visible list
                updateGoalStatus();                // refresh banner
                maybeSendGoalReachedSms(lbs);      // try sms if goal reached
            } catch (NumberFormatException e) {
                toast(getString(R.string.enter_valid_weight));
            }
        });

        // initial UI
        renderList();
        updateGoalStatus();
    }

    // Rebuilds the list from database rows (newest first)
    private void renderList() {
        listContainer.removeAllViews();
        try (Cursor c = db.getAllWeights(userId)) {
            while (c.moveToNext()) {
                long rowId = c.getLong(c.getColumnIndexOrThrow("_id"));
                String date = c.getString(c.getColumnIndexOrThrow("entry_date"));
                double lbs = c.getDouble(c.getColumnIndexOrThrow("weight_lbs"));
                addRowView(rowId, lbs, date);
            }
        }
    }

    // Adds one row: weight | date | delete button
    private void addRowView(long rowId, double lbs, String date) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int pad = dp(8);
        row.setPadding(0, pad, 0, pad);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvWeight = new TextView(this);
        tvWeight.setLayoutParams(weighted(1f));
        tvWeight.setText(String.valueOf(lbs));

        TextView tvDate = new TextView(this);
        tvDate.setLayoutParams(weighted(1f));
        tvDate.setText(date);

        // Small delete button with a confirm dialog to avoid accidental removals
        Button btnDelete = new Button(this, null, android.R.attr.buttonStyleSmall);
        btnDelete.setText(getString(R.string.delete));
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage(getString(R.string.delete_entry_q))
                .setPositiveButton(getString(R.string.delete), (d, w) -> {
                    db.deleteWeight(rowId);
                    renderList();
                    updateGoalStatus();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());

        row.addView(tvWeight);
        row.addView(tvDate);
        row.addView(btnDelete);

        listContainer.addView(row);
    }

    // Set Goal button → popup with one field and Set button
    public void onSetGoalClick(View v) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_goal, null);
        EditText input = dialogView.findViewById(R.id.etGoal);
        Button btnSet = dialogView.findViewById(R.id.btnSetGoalDialog);

        // Build dialog first so the X button can dismiss it
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Header: title + close X
        TextView tvHeader = dialogView.findViewById(R.id.tvDialogTitle);
        android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        if (tvHeader != null) tvHeader.setText(getString(R.string.set_goal_title));
        if (btnClose != null) btnClose.setOnClickListener(click -> dlg.dismiss());

        // Preload current goal for easy edits
        double currentGoal = db.getGoalWeight(userId);
        if (currentGoal > 0) {
            input.setText(String.valueOf(currentGoal));
            input.setSelection(input.getText().length());
        }

        dlg.show();

        // Save goal on click with a quick number check
        btnSet.setOnClickListener(click -> {
            String goalStr = input.getText().toString().trim();
            if (goalStr.isEmpty()) { toast(getString(R.string.enter_valid_number)); return; }
            try {
                double goal = Double.parseDouble(goalStr);
                db.setGoalWeight(userId, goal);
                toast(getString(R.string.goal_updated_to, goal));
                updateGoalStatus();
                dlg.dismiss();
            } catch (NumberFormatException e) {
                toast(getString(R.string.enter_valid_number));
            }
        });
    }

    // Updates banner text (goal line + current/to-go line)
    private void updateGoalStatus() {
        double goal = db.getGoalWeight(userId);

        if (goal <= 0) {
            tvGoalWeightLine.setText(getString(R.string.goal_not_set));
        } else {
            tvGoalWeightLine.setText(getString(R.string.goal_line, goal));
        }

        String statusText = getString(R.string.current_dash);
        try (Cursor c = db.getAllWeights(userId)) {
            if (c.moveToFirst()) {
                double current = c.getDouble(c.getColumnIndexOrThrow("weight_lbs"));
                if (goal > 0) {
                    double diff = current - goal;
                    String suffix = (diff > 0)
                            ? String.format(Locale.getDefault(), getString(R.string.to_go_suffix), diff)
                            : String.format(Locale.getDefault(), getString(R.string.past_goal_suffix), Math.abs(diff));
                    statusText = getString(R.string.current_line, current) + suffix;
                } else {
                    statusText = getString(R.string.current_line, current);
                }
            }
        }
        tvGoalStatusLine.setText(statusText);
    }

    // Checks if the new weight meets/beats the goal and handles permission flow for SMS
    private void maybeSendGoalReachedSms(double newWeight) {
        double goal = db.getGoalWeight(userId);
        if (goal <= 0) return;        // no goal set, skip
        if (newWeight > goal) return; // not at/under goal yet

        int granted = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.SEND_SMS);

        if (granted == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Permission is good → send the alert now
            sendGoalSms(newWeight, goal);
        } else {
            // Ask for permission one time here; app still works if they say no
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.SEND_SMS},
                    REQ_SMS
            );
        }
    }

    // Actually sends the SMS alert when the goal is reached
    private void sendGoalSms(double current, double goal) {
        String msg = String.format(java.util.Locale.getDefault(),
                "Goal reached! Current: %.1f lb (goal %.1f lb).", current, goal);
        android.telephony.SmsManager.getDefault().sendTextMessage(
                ALERT_NUMBER, null, msg, null, null);
        toast("SMS alert sent.");
    }

    // Handles the user's choice on the permission popup
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SMS) {
            if (grantResults.length > 0 &&
                    grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Not auto-sending here, since we don't know the last weight entered
                toast("SMS permission granted. Add a new entry to trigger alert.");
            } else {
                toast("SMS permission denied. App continues without SMS.");
            }
        }
    }

    // Logs the user out and returns to the login screen
    public void onLogoutClick(View view) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // helpers
    @SuppressWarnings("SameParameterValue")
    private LinearLayout.LayoutParams weighted(float w) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.weight = w;
        return p;
    }

    @SuppressWarnings("SameParameterValue")
    private int dp(int d) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(d * density);
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}