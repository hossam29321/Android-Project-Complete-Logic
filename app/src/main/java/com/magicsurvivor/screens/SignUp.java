package com.magicsurvivor.screens;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.magicsurvivor.R;


public class SignUp extends AppCompatActivity {
    private final FirebaseAuth refAuth = FirebaseAuth.getInstance();
    private DatabaseReference mDatabase;
    private EditText eTEmail;
    private EditText eTPass;
    private EditText eTConfirmPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Update IDs to match the XML
        eTEmail = findViewById(R.id.emailInput);
        eTPass = findViewById(R.id.passwordInput);
        eTConfirmPass = findViewById(R.id.confirmPasswordInput);

        // Set up click listeners
        MaterialButton signUpButton = findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(v -> registerUser(v));

        TextView logInText = findViewById(R.id.logInText);
        logInText.setOnClickListener(v -> backToLogin(v));
    }

    public void registerUser(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();
        String confirmPass = eTConfirmPass.getText().toString();

        if (email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        } else if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
        } else if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        } else {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Connecting");
            pd.setMessage("Creating account...");
            pd.show();

            refAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            pd.dismiss();

                            if (task.isSuccessful()) {
                                Log.i("SignUp", "createUserWithEmailAndPassword:success");
                                FirebaseUser user = refAuth.getCurrentUser();
                                // Send verification email
                                user.sendEmailVerification()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.i("SignUp", "Verification email sent successfully");
                                                    Toast.makeText(SignUp.this, "Account created! Verification email sent to " + email, Toast.LENGTH_LONG).show();
                                                    Toast.makeText(SignUp.this, "Check your inbox (and spam folder) for the verification link", Toast.LENGTH_LONG).show();
                                                    // Sign out the user - they must verify email first
                                                    refAuth.signOut();
                                                    Intent intent = new Intent(SignUp.this, Login.class);
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    Log.e("SignUp", "Failed to send verification email: " + task.getException());
                                                    Toast.makeText(SignUp.this, "Failed to send verification email. Please try again.", Toast.LENGTH_LONG).show();
                                                    // Delete the account if we can't send verification
                                                    user.delete();
                                                }
                                            }
                                        });
                            } else {
                                Exception exp = task.getException();
                                if (exp instanceof FirebaseAuthWeakPasswordException) {
                                    Toast.makeText(SignUp.this, "Password too weak (minimum 6 characters)", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(SignUp.this, "Email already registered", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(SignUp.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseNetworkException) {
                                    Toast.makeText(SignUp.this, "Network error, Please check your connection", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SignUp.this, "An error occurred, Please try again later", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void backToLogin(View view) {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }
}