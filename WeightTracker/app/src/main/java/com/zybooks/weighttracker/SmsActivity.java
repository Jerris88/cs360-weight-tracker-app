package com.zybooks.weighttracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Handles SMS permission requests and sends a test message if access is granted
@SuppressWarnings("FieldCanBeLocal") // keep fields; easier to reuse in callbacks
public class SmsActivity extends AppCompatActivity {

    private static final int REQ_SEND_SMS = 100;

    // UI references
    private TextView tvPermissionStatus;
    private EditText etPhone;
    private Button btnRequestPermission, btnSendTest, btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Loads the layout that shows SMS permission status and test message options
        setContentView(R.layout.activity_sms);

        // Connect layout views
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        etPhone = findViewById(R.id.etPhone);
        btnRequestPermission = findViewById(R.id.btnRequestPermission);
        btnSendTest = findViewById(R.id.btnSendTest);
        btnDone = findViewById(R.id.btnDone); // added in XML

        // Give a quick hint about what to do after
        Toast.makeText(this, "When you’re done, tap Done to return and log in.", Toast.LENGTH_LONG).show();

        // Check permission at launch so the UI can reflect the right state
        updatePermissionStatus();

        // Button: request SMS permission (or explain why it can't show again)
        btnRequestPermission.setOnClickListener(v -> {
            Toast.makeText(this, "Requesting SMS permission…", Toast.LENGTH_SHORT).show();

            // If already granted, no need to ask again
            if (isSmsGranted()) {
                Toast.makeText(this, "Already allowed", Toast.LENGTH_SHORT).show();
                updatePermissionStatus();
                return;
            }

            // If the system says we should show a rationale, show a quick message first
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                Toast.makeText(this, "This lets the app send goal alerts by text.", Toast.LENGTH_LONG).show();
            }

            // Request the permission (system dialog shows unless in 'Don't ask again')
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    REQ_SEND_SMS);
        });

        // Button: send a test SMS if permission is granted
        btnSendTest.setOnClickListener(v -> sendTestMessage());

        // Done button: returns user back to login with the username prefilled
        btnDone.setText("Done — Return to Login");
        btnDone.setOnClickListener(v -> {
            // pull the username we passed in from registration
            String prefill = getIntent().getStringExtra("prefill_username");

            Intent back = new Intent(this, MainActivity.class);
            back.putExtra("prefill_username", prefill);
            back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(back);
            finish(); // close SmsActivity
        });
    }

    private boolean isSmsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Updates the status text and enables/disables the test button
    private void updatePermissionStatus() {
        boolean granted = isSmsGranted();

        tvPermissionStatus.setText(granted
                ? getString(R.string.sms_permission_granted)
                : getString(R.string.sms_permission_not_granted));

        tvPermissionStatus.setTextColor(getColor(
                granted ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));

        btnSendTest.setEnabled(granted);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_SEND_SMS) {
            boolean granted = grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // If user tapped "Don't ask again", the dialog won't show anymore.
                boolean canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.SEND_SMS);

                if (!canAskAgain) {
                    Toast.makeText(this,
                            "Permission permanently denied. Enable SMS in App Settings.",
                            Toast.LENGTH_LONG).show();
                    showOpenSettingsDialog();
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
                }
            }
            updatePermissionStatus();
        }
    }

    // Sends a short test SMS message if permission has been approved
    private void sendTestMessage() {
        String phone = etPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isSmsGranted()) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phone, null,
                    "Test alert: SMS notifications are working!",
                    null, null);

            Toast.makeText(this, "Test SMS sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Offers to open App Settings when user chose "Don't ask again"
    private void showOpenSettingsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Enable SMS in Settings")
                .setMessage("To send goal alerts by text, allow SMS in App Settings.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}