package com.zybooks.weighttracker;

import android.app.Dialog;
import android.content.Intent; // open SmsActivity
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // for the X icon
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

// Dialog that handles new account creation for the app
public class RegisterDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Load the layout for this dialog (header is included in the layout)
        View v = getLayoutInflater().inflate(R.layout.dialog_register, null);

        // Header setup: title and close button
        TextView tvHeader = v.findViewById(R.id.tvDialogTitle);
        ImageButton btnClose = v.findViewById(R.id.btnCloseDialog);
        if (tvHeader != null) tvHeader.setText(getString(R.string.create_account)); // sets the title text
        if (btnClose != null) btnClose.setOnClickListener(view -> dismiss());       // closes the dialog

        // Input fields for registration
        EditText etFirst = v.findViewById(R.id.etFirst);
        EditText etLast  = v.findViewById(R.id.etLast);
        EditText etEmail = v.findViewById(R.id.etEmail);
        EditText etUser  = v.findViewById(R.id.etUsernameReg);
        EditText etPass  = v.findViewById(R.id.etPasswordReg);
        EditText etPass2 = v.findViewById(R.id.etPasswordReg2);
        Spinner  spSecQ  = v.findViewById(R.id.spSecQ);
        EditText etSecA  = v.findViewById(R.id.etSecA);
        TextView tvError = v.findViewById(R.id.tvRegError);
        Button   btnReg  = v.findViewById(R.id.btnRegisterForm);

        // Builds the alert dialog with cancel as a fallback exit option
        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setView(v)
                .setNegativeButton("Cancel", null)
                .create();

        // Register button click logic
        btnReg.setOnClickListener(view -> {
            // Helper to display inline errors
            java.util.function.Consumer<String> showErr = msg -> {
                tvError.setText(msg);
                tvError.setVisibility(View.VISIBLE);
            };
            tvError.setVisibility(View.GONE); // hides any previous error

            // Read and clean input values
            String first = txt(etFirst);
            String last  = txt(etLast);
            String email = txt(etEmail).toLowerCase();
            String user  = txt(etUser);
            String p1    = etPass.getText().toString();
            String p2    = etPass2.getText().toString();
            String q     = spSecQ.getSelectedItem() == null ? "" : spSecQ.getSelectedItem().toString();
            String a     = txt(etSecA);

            // Basic validation for required fields
            if (TextUtils.isEmpty(first) || TextUtils.isEmpty(last) ||
                    TextUtils.isEmpty(email) || TextUtils.isEmpty(user) ||
                    TextUtils.isEmpty(p1)    || TextUtils.isEmpty(p2)   ||
                    TextUtils.isEmpty(q)     || TextUtils.isEmpty(a)) {
                showErr.accept(getString(R.string.error_fill_all_fields));
                return;
            }

            // Check formatting and simple rules
            if (!email.matches(".+@.+\\..+")) {
                showErr.accept("Enter a valid email");
                return;
            }
            if (user.length() < 3) {
                showErr.accept("Username must be 3+ characters");
                return;
            }
            if (!p1.equals(p2) || p1.length() < 6) {
                showErr.accept("Passwords must match and be 6+ chars");
                return;
            }

            // Save new user to the database (using try-with-resources for safety)
            try (DatabaseHelper db = new DatabaseHelper(requireContext())) {
                // Check for duplicates before creating
                if (db.usernameExists(user)) {
                    showErr.accept("Username already exists");
                    return;
                }
                if (db.emailExists(email)) {
                    showErr.accept("Email already exists");
                    return;
                }

                // Insert new user record
                long id = db.createUserFull(first, last, email, user, p1, q, a);
                if (id > 0) {
                    Toast.makeText(requireContext(), "Account created", Toast.LENGTH_SHORT).show();

                    // Opens the SMS screen right after registration so the user can allow or deny texting.
                    // Pass the new username so the login screen can prefill it later.
                    Intent i = new Intent(requireContext(), SmsActivity.class);
                    i.putExtra("prefill_username", user);
                    startActivity(i);

                    dismiss();
                } else {
                    showErr.accept("Could not create account");
                }
            }
        });

        return dlg;
    }

    // Helper to trim user input safely
    private String txt(EditText e) {
        return e.getText().toString().trim();
    }
}