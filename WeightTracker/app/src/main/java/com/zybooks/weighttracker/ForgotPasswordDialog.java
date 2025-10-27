package com.zybooks.weighttracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

// Dialog that walks through the 3-step password reset process
public class ForgotPasswordDialog extends DialogFragment {

    // Holds the username found during the email lookup step
    private String currentUsername = null;

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
        // Loads the forgot password layout for this dialog
        View v = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);

        // Header section with title and close button
        TextView tvHeader = v.findViewById(R.id.tvDialogTitle);
        ImageButton btnClose = v.findViewById(R.id.btnCloseDialog);
        if (tvHeader != null) tvHeader.setText(getString(R.string.forgot_password_title));
        if (btnClose != null) btnClose.setOnClickListener(x -> dismiss());

        // Step containers (email → question → reset)
        LinearLayout sectionEmail = v.findViewById(R.id.sectionEmail);
        LinearLayout sectionQuestion = v.findViewById(R.id.sectionQuestion);
        LinearLayout sectionReset = v.findViewById(R.id.sectionReset);

        // Shared error message view used across all steps
        TextView tvError = v.findViewById(R.id.tvFpError);

        // Step 1 fields (email input + next button)
        EditText etEmail = v.findViewById(R.id.etEmailLookup);
        Button btnEmailNext = v.findViewById(R.id.btnEmailNext);

        // Step 2 fields (security question + answer)
        TextView tvQuestion = v.findViewById(R.id.tvSecQuestion);
        EditText etAnswer = v.findViewById(R.id.etSecAnswer);
        Button btnAnswerVerify = v.findViewById(R.id.btnAnswerVerify);

        // Step 3 fields (new password + confirm + reset button)
        EditText etP1 = v.findViewById(R.id.etNewPass);
        EditText etP2 = v.findViewById(R.id.etNewPass2);
        Button btnReset = v.findViewById(R.id.btnResetPassword);

        // Builds the alert dialog container for the layout
        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.reset_password))
                .setView(v)
                .setNegativeButton(R.string.close, null)
                .create();

        // === Step 1: email lookup ===
        btnEmailNext.setOnClickListener(view -> {
            hide(tvError);

            String email = etEmail.getText().toString().trim().toLowerCase();
            // Simple check to make sure email isn't blank and looks valid
            if (TextUtils.isEmpty(email) || !email.matches(".+@.+\\..+")) {
                showErr(tvError, getString(R.string.error_valid_email));
                return;
            }

            // Opens database and searches for a username linked to this email
            try (DatabaseHelper db = new DatabaseHelper(requireContext())) {
                String uname = db.getUsernameByEmail(email);
                if (uname == null) {
                    showErr(tvError, getString(R.string.error_email_not_found));
                    return;
                }

                // Fetches the stored security question for that username
                String q = db.getSecurityQuestion(uname);
                if (TextUtils.isEmpty(q)) {
                    showErr(tvError, getString(R.string.error_no_security_question));
                    return;
                }

                // Saves username for later steps and moves to question screen
                currentUsername = uname;
                tvQuestion.setText(q);
                hide(sectionEmail);
                show(sectionQuestion);
            }
        });

        // === Step 2: verify security answer ===
        btnAnswerVerify.setOnClickListener(view -> {
            hide(tvError);

            // User must have passed the email step first
            if (currentUsername == null) {
                showErr(tvError, getString(R.string.start_with_email));
                return;
            }

            String entered = etAnswer.getText().toString().trim();
            if (TextUtils.isEmpty(entered)) {
                showErr(tvError, getString(R.string.error_fill_all_fields));
                return;
            }

            // Compares answer entered to the one stored in the database
            try (DatabaseHelper db = new DatabaseHelper(requireContext())) {
                String stored = db.getSecurityAnswer(currentUsername);
                if (!entered.equalsIgnoreCase(stored)) {
                    showErr(tvError, getString(R.string.error_bad_security_answer));
                    return;
                }

                // Answer is correct → move to reset password screen
                hide(sectionQuestion);
                show(sectionReset);
            }
        });

        // === Step 3: reset password ===
        btnReset.setOnClickListener(view -> {
            hide(tvError);

            // Must have valid username from earlier steps
            if (currentUsername == null) {
                showErr(tvError, getString(R.string.start_with_email));
                return;
            }

            String p1 = etP1.getText().toString();
            String p2 = etP2.getText().toString();

            // Make sure both password fields are filled out
            if (TextUtils.isEmpty(p1) || TextUtils.isEmpty(p2)) {
                showErr(tvError, getString(R.string.error_fill_all_fields));
                return;
            }
            // Passwords must match and meet length requirement
            if (!p1.equals(p2) || p1.length() < 6) {
                showErr(tvError, getString(R.string.error_password_mismatch_len));
                return;
            }

            // Updates password in the database if all checks pass
            try (DatabaseHelper db = new DatabaseHelper(requireContext())) {
                boolean ok = db.updatePassword(currentUsername, p1);
                if (ok) {
                    Toast.makeText(requireContext(), R.string.password_updated, Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    showErr(tvError, getString(R.string.update_failed));
                }
            }
        });

        return dlg;
    }

    // Shows an error message inside the shared TextView
    private void showErr(TextView tv, String msg) {
        tv.setText(msg);
        tv.setVisibility(View.VISIBLE);
    }

    // Helper to make a view visible
    private void show(View v) { v.setVisibility(View.VISIBLE); }

    // Helper to hide a view
    private void hide(View v) { v.setVisibility(View.GONE); }
}