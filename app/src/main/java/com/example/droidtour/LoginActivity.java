package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.client.ClientMainActivity;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// Firebase imports
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister, btnGoogleSignIn;
    private TextView tvForgotPassword;
    private PreferencesManager prefsManager;

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        prefsManager = new PreferencesManager(this);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Redirección en caso se encuentre sesión activa
        if (prefsManager.isLoggedIn() && !prefsManager.getUserType().isEmpty()) {
            redirigirSegunRol();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            CharSequence emailCs = etEmail.getText();
            CharSequence passwordCs = etPassword.getText();
            String email = emailCs != null ? emailCs.toString().trim() : "";
            String password = passwordCs != null ? passwordCs.toString().trim() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);
            authenticateUser(email, password);
        });

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RoleSelectionActivity.class)));

        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));

        // Google Sign-In
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Error en autenticación con Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void firebaseAuthWithGoogle(String idToken) {
        btnGoogleSignIn.setEnabled(false);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    btnGoogleSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserExistsInFirestore(user);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error en autenticación", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserExistsInFirestore(FirebaseUser user) {
        String userId = user.getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // Usuario existe - redirigir según su rol
                        DocumentSnapshot doc = task.getResult();
                        String userType = doc.getString("userType");
                        if (userType != null && !userType.isEmpty()) {
                            handleExistingUser(user, userType);
                        } else {
                            // Usuario existe pero sin rol - redirigir a selección de rol
                            redirectToRoleSelection(user);
                        }
                    } else {
                        // Usuario nuevo - redirigir a selección de rol
                        redirectToRoleSelection(user);
                    }
                });
    }

    private void redirectToRoleSelection(FirebaseUser user) {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.putExtra("googleUser", true);
        intent.putExtra("userEmail", user.getEmail());
        intent.putExtra("userName", user.getDisplayName());
        intent.putExtra("userPhoto", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        startActivity(intent);
        finish();
    }
    private void handleExistingUser(FirebaseUser user, String userType) {
        String uid = user.getUid();
        String userEmail = user.getEmail() != null ? user.getEmail() : "";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";

        prefsManager.saveUserData(uid, displayName, userEmail, "", userType);
        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
        prefsManager.marcarPrimeraVezCompletada();

        // VERIFICAR STATUS DEL GUÍA ANTES DE REDIRIGIR
        if ("GUIDE".equals(userType)) {
            checkGuideApprovalStatus(uid);
        } else {
            redirigirSegunRol();
            finish();
        }
    }

    private void checkGuideApprovalStatus(String userId) {
        FirebaseFirestore.getInstance().collection("user_roles")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();

                        // Verificar si existe el campo 'guide'
                        if (doc.contains("guide")) {
                            Map<String, Object> guideData = (Map<String, Object>) doc.get("guide");
                            if (guideData != null) {
                                String status = (String) guideData.get("status");

                                if ("active".equals(status)) {
                                    // Guía aprobado - redirigir al dashboard
                                    redirigirSegunRol();
                                    finish();
                                } else {
                                    // Guía no aprobado - redirigir a pantalla de espera
                                    redirectToApprovalPending();
                                }
                            } else {
                                // No hay datos de guía - redirigir a pantalla de espera
                                redirectToApprovalPending();
                            }
                        } else {
                            // No existe el campo guide - redirigir a pantalla de espera
                            redirectToApprovalPending();
                        }
                    } else {
                        // Error al obtener datos - redirigir a pantalla de espera por seguridad
                        redirectToApprovalPending();
                    }
                });
    }

    private void checkUserApprovalStatus() {
        String userType = prefsManager.getUserType();
        String userId = prefsManager.getUserId();

        if ("GUIDE".equals(userType) && !userId.isEmpty()) {
            checkGuideApprovalStatus(userId);
        } else {
            redirigirSegunRol();
            finish();
        }
    }

    private void redirectToApprovalPending() {
        Intent intent = new Intent(this, GuideApprovalPendingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }



    private void authenticateUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        btnLogin.setEnabled(true);
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Guardar información mínima en Preferences
                                String uid = user.getUid();
                                String userEmail = user.getEmail() != null ? user.getEmail() : email;

                                // Intent: obtener rol real desde Firestore si existe
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("users").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(Task<DocumentSnapshot> task2) {
                                        String role;
                                        if (task2.isSuccessful() && task2.getResult() != null && task2.getResult().exists()) {
                                            DocumentSnapshot doc = task2.getResult();
                                            role = doc.getString("userType");
                                            if (role == null || role.isEmpty()) {
                                                role = inferUserTypeFromEmail(userEmail);
                                            }
                                        } else {
                                            // No hay documento de perfil, usar inferencia
                                            role = inferUserTypeFromEmail(userEmail);
                                        }

                                        prefsManager.saveUserData(uid, "Usuario", userEmail, "", role);
                                        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                                        prefsManager.marcarPrimeraVezCompletada();

                                        // Redirigir según rol
                                        redirigirSegunRol();
                                        finish();
                                    }
                                });

                            } else {
                                Toast.makeText(LoginActivity.this, "Error al obtener usuario", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            String message = task.getException() != null ? task.getException().getMessage() : "Autenticación fallida";
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Inferir tipo de usuario desde el email para compatibilidad con los mocks existentes.
     * Recomendado: reemplazar por consulta a Firestore donde cada usuario tenga su 'role'.
     */
    private String inferUserTypeFromEmail(String email) {
        if (email == null) return "CLIENT";
        String lower = email.toLowerCase();
        if (lower.contains("superadmin@droidtour.com") || lower.contains("superadmin")) return "SUPERADMIN";
        if (lower.contains("admin@tours.com") || lower.contains("admin")) return "ADMIN";
        if (lower.contains("guia@tours.com") || lower.contains("guia") || lower.contains("guide")) return "GUIDE";
        // Por defecto CLIENT
        return "CLIENT";
    }

    private void redirigirSegunRol(){
        String tipoUsuario = prefsManager.getUserType();
        Intent intent; // no inicializar a null para evitar warning

        switch (tipoUsuario){
            case "SUPERADMIN":
                intent = new Intent(this, SuperadminMainActivity.class);
                break;
            case "ADMIN":
                intent = new Intent(this, TourAdminMainActivity.class);
                break;
            case "GUIDE":
                intent = new Intent(this, TourGuideMainActivity.class);
                break;
            case "CLIENT":
                intent = new Intent(this, ClientMainActivity.class);
                break;
            default:
                intent = new Intent(this, LoginActivity.class);
                break;
        }

        startActivity(intent);
    }
}
