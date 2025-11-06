package com.example.droidtour.client;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ClientCreatePasswordActivity extends AppCompatActivity {
    private TextInputEditText etPassword, etRepeatPassword;
    private TextView tvPasswordError;
    private MaterialButton btnSiguiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_create_password);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        findViewById(R.id.tvRegresar).setOnClickListener(v -> finish());

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswords();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etPassword.addTextChangedListener(watcher);
        etRepeatPassword.addTextChangedListener(watcher);

        btnSiguiente.setOnClickListener(v -> {
            // Aquí puedes continuar con el flujo de registro
            finish();
        });
    }

    private void validatePasswords() {
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String repeat = etRepeatPassword.getText() != null ? etRepeatPassword.getText().toString() : "";
        String error = getPasswordError(password, repeat);
        if (error == null) {
            tvPasswordError.setVisibility(TextView.GONE);
            btnSiguiente.setEnabled(true);
        } else {
            tvPasswordError.setText(error);
            tvPasswordError.setVisibility(TextView.VISIBLE);
            btnSiguiente.setEnabled(false);
        }
    }

    private String getPasswordError(String password, String repeat) {
        if (password.isEmpty() || repeat.isEmpty()) {
            return "Debes ingresar ambas contraseñas.";
        }
        if (!password.equals(repeat)) {
            return "Las contraseñas no coinciden.";
        }
        if (password.length() < 8) {
            return "La contraseña debe tener al menos 8 caracteres.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Debe contener al menos una letra minúscula.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Debe contener al menos una letra mayúscula.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Debe contener al menos un número.";
        }
        if (!password.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/|`~].*")) {
            return "Debe contener al menos un caracter especial.";
        }
        return null;
    }
}
