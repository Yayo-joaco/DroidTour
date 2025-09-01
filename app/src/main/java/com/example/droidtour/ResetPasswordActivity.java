package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword, etConfirmPassword;
    private MaterialButton btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnReset = findViewById(R.id.btn_reset_password);

        btnReset.setOnClickListener(v -> {
            String p1 = etNewPassword.getText() == null ? "" : etNewPassword.getText().toString();
            String p2 = etConfirmPassword.getText() == null ? "" : etConfirmPassword.getText().toString();
            if (p1.isEmpty()) { etNewPassword.setError("Ingrese nueva contraseña"); return; }
            if (!p1.equals(p2)) { etConfirmPassword.setError("Las contraseñas no coinciden"); return; }
            Toast.makeText(this, "Contraseña restablecida", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
