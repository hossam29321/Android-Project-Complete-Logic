package com.magicsurvivor.screens;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
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
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.magicsurvivor.R;
//import com.magicsurvivor.User; testcommit again


public class Login extends AppCompatActivity {
    private final FirebaseAuth refAuth= FirebaseAuth.getInstance();
    private EditText eTEmail;
    private EditText eTPass;
    private CheckBox rememberMe;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Update IDs to match the XML
        eTEmail = findViewById(R.id.emailInput);
        eTPass = findViewById(R.id.passwordInput);
        rememberMe = findViewById(R.id.rememberMeCheckbox);

        // Set up click listeners
        MaterialButton loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(v -> createUser(v));

        TextView signUpText = findViewById(R.id.signUpText);
        signUpText.setOnClickListener(v -> goToSignUp(v));

        // Load saved credentials if "Remember Me" was checked
        loadSavedCredentials();
    }

    public void goToSignUp(View view) {
        Intent intent = new Intent(this, SignUp.class);
        startActivity(intent);
    }

    public void createUser(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        } else {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Connecting");
            pd.setMessage("Logging in...");
            pd.show();

            // Try to sign in with email and password
            refAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            pd.dismiss();

                            if (task.isSuccessful()) {
                                Log.i("MainActivity", "signInWithEmailAndPassword:success");
                                FirebaseUser user = refAuth.getCurrentUser();


                                // Check if email is verified
                                if (user.isEmailVerified()) {
                                    Toast.makeText(Login.this, "Login successful", Toast.LENGTH_SHORT).show();

                                    // Save credentials if "Remember Me" is checked
                                    if (rememberMe.isChecked()) {
                                        saveCredentials(email, pass);
                                    } else {
                                        clearSavedCredentials();
                                    }
                                    String uid=user.getUid();
                                    db=FirebaseDatabase.getInstance().getReference().child(uid).child("money");
                                    db.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String money=" "+snapshot.getValue().toString()+"   ";
                                            Intent intent = new Intent(Login.this, MainMenu.class);
                                            intent.putExtra("money",money);
                                            intent.putExtra("userId", uid);
                                            startActivity(intent);
                                            finish();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });


                                } else {
                                    Toast.makeText(Login.this, "Please verify your email first. Check your inbox for verification link", Toast.LENGTH_LONG).show();
                                    refAuth.signOut();
                                }
                            }
                            else {
                                Exception exp = task.getException();
                                if (exp instanceof FirebaseAuthInvalidUserException) {
                                    Toast.makeText(Login.this, "Email does not exist", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(Login.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseNetworkException) {
                                    Toast.makeText(Login.this, "Network error, Please check your connection", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(Login.this, "An error occurred, Please try again later", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("password", password);
        editor.apply();
    }

    private void loadSavedCredentials() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String savedEmail = sharedPreferences.getString("email", "");
        String savedPassword = sharedPreferences.getString("password", "");

        if (!savedEmail.isEmpty()) {
            eTEmail.setText(savedEmail);
            eTPass.setText(savedPassword);
            rememberMe.setChecked(true);
        }
    }

    private void clearSavedCredentials() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("email");
        editor.remove("password");
        editor.apply();
    }
}