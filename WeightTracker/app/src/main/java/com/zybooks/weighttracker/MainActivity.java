package com.zybooks.weighttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // input fields and error text
    private EditText usernameEt, passwordEt;
    private TextView errorTv;

    // local storage for login sessions
    private SharedPreferences prefs;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // keeps layout from overlapping system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // connect UI elements
        usernameEt = findViewById(R.id.etUsername);
        passwordEt = findViewById(R.id.etPassword);
        errorTv = findViewById(R.id.tvLoginError);

        // create or open the session storage file
        prefs = getSharedPreferences("session", MODE_PRIVATE);
        db = new DatabaseHelper(this);

        // If user sent back from SmsActivity with a username, prefill and nudge to log in
        String prefill = getIntent().getStringExtra("prefill_username");
        if (prefill != null && !prefill.isEmpty()) {
            usernameEt.setText(prefill);
            passwordEt.requestFocus();
            Toast.makeText(this, "Account created. Log in with your new password.", Toast.LENGTH_SHORT).show();
        }

        // skip login if already logged in
        long userId = prefs.getLong("userId", -1);
        if (userId > 0) {
            startActivity(new Intent(this, TrackerActivity.class));
            finish();
        }
    }

    // Login button
    public void onLoginClick(View v) {
        hideError();
        String u = txt(usernameEt);
        String p = txt(passwordEt);

        if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
            showError("Please enter username and password.");
            return;
        }

        long userId = db.authenticate(u, p);
        if (userId > 0) {
            prefs.edit().putLong("userId", userId).apply();
            startActivity(new Intent(this, TrackerActivity.class));
            finish();
        } else {
            showError(getString(R.string.error_invalid_login));
        }
    }

    // Create Account button
    public void onCreateAccountClick(View v) {
        hideError();
        showRegisterDialog();
    }

    // Forgot Username link
    public void onForgotUsernameClick(View v) {
        hideError();
        showForgotUsernameDialog();
    }

    // Forgot Password link
    public void onForgotPasswordClick(View v) {
        hideError();
        showForgotPasswordDialog();
    }

    // Dialog openers
    private void showRegisterDialog() {
        new RegisterDialog().show(getSupportFragmentManager(), "register");
    }

    private void showForgotUsernameDialog() {
        new ForgotUsernameDialog().show(getSupportFragmentManager(), "forgot_username");
    }

    private void showForgotPasswordDialog() {
        new ForgotPasswordDialog().show(getSupportFragmentManager(), "forgot_password");
    }

    // error helpers
    private void showError(String msg) {
        if (errorTv != null) {
            errorTv.setText(msg);
            errorTv.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void hideError() {
        if (errorTv != null) errorTv.setVisibility(View.INVISIBLE);
    }

    // small input helper
    private String txt(EditText et) {
        return et.getText().toString().trim();
    }
}