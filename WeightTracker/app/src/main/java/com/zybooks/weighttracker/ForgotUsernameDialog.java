package com.zybooks.weighttracker;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

// Dialog that helps a user find their username by email
public class ForgotUsernameDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Loads the dialog layout
        View v = getLayoutInflater().inflate(R.layout.dialog_forgot_username, null);

        // Header with title and close button
        TextView tvHeader = v.findViewById(R.id.tvDialogTitle);
        android.widget.ImageButton btnClose = v.findViewById(R.id.btnCloseDialog);
        if (tvHeader != null) tvHeader.setText(getString(R.string.find_username_title));
        if (btnClose != null) btnClose.setOnClickListener(x -> dismiss());

        // Main views for input, action, and messages
        EditText etEmail  = v.findViewById(R.id.etEmailLookup);
        Button btnLookup  = v.findViewById(R.id.btnLookupUsername);
        TextView tvError  = v.findViewById(R.id.tvFuError);
        TextView tvResult = v.findViewById(R.id.tvUsernameResult);

        // Builds the alert dialog shell around the layout
        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.find_username_title))
                .setView(v)
                .setNegativeButton(R.string.close, null)
                .create();

        // Button: look up username by email
        btnLookup.setOnClickListener(view -> {
            // Hide any previous messages before validating
            tvError.setVisibility(View.GONE);
            tvResult.setVisibility(View.GONE);

            // Basic email check (not perfect, just catches obvious mistakes)
            String email = etEmail.getText().toString().trim().toLowerCase();
            if (TextUtils.isEmpty(email) || !email.matches(".+@.+\\..+")) {
                tvError.setText(getString(R.string.error_valid_email));
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            // Open the database helper and try to find a username for this email
            try (DatabaseHelper db = new DatabaseHelper(requireContext())) {
                String username = db.getUsernameByEmail(email);
                if (username == null) {
                    // No match found for that email
                    tvError.setText(getString(R.string.error_email_not_found));
                    tvError.setVisibility(View.VISIBLE);
                    return;
                }

                // Show the found username
                tvResult.setText(getString(R.string.username_is_label, username));
                tvResult.setVisibility(View.VISIBLE);
            }
        });

        return dlg;
    }
}