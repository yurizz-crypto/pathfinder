package com.prototype.pathfinder.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.data.DBManager;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private DBManager dbManager;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        dbManager = new DBManager(this);
        dbManager.open();
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pw = etPassword.getText().toString();
            if (dbManager.loginUser(email, pw)) {
                prefs.edit().putString("user_email", email).apply();
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}