package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.client.ClientMainActivity;
import com.example.droidtour.superadmin.SuperadminMainActivity;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.models.UserSession;
import com.example.droidtour.models.User;
import com.example.droidtour.firebase.FirestoreManager;
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
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
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
        btnGoogleSignIn.setEnabled(false);

        // Forzar selector de cuenta: cerrar la sesión actual del cliente antes de iniciar el flujo
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
            btnGoogleSignIn.setEnabled(true);
        });
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

        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Usuario existe - redirigir según su rol
                User userObj = (User) result;
                String userType = userObj.getUserType();
                if (userType != null && !userType.isEmpty()) {
                    handleExistingUser(user, userType);
                } else {
                    // Usuario existe pero sin rol - redirigir a selección de rol
                    redirectToRoleSelection(user);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Usuario nuevo o error - redirigir a selección de rol
                Log.w(TAG, "Usuario no encontrado en Firestore o error al obtener datos para userId: " + user.getUid() + " - " + e.getMessage());
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
        
        // 🔥 OBTENER DATOS COMPLETOS DESDE FIRESTORE PRIMERO usando FirestoreManager
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserById(uid, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User userObj = (User) result;
                // Revisar si la cuenta está desactivada por admin o tiene status no activo
                Boolean isActiveField = userObj.getActive();
                String statusField = userObj.getStatus();

                if ((isActiveField != null && !isActiveField) ||
                                (statusField != null && ("inactive".equalsIgnoreCase(statusField) || "suspended".equalsIgnoreCase(statusField)))) {
                            // For safety, sign out and redirect to disabled screen
                            redirectToUserDisabled(uid, "Tu cuenta ha sido desactivada. Contacta con soporte.");
                            return;
                        }

                        // Extraer datos completos del usuario desde el objeto User
                        String fullName = userObj.getFullName();
                        String displayName = fullName != null ? fullName : 
                            (userObj.getFirstName() + " " + userObj.getLastName());
                        String userEmail = userObj.getEmail();
                        String phoneNumber = userObj.getPhoneNumber();

                        // Guardar datos COMPLETOS en PreferencesManager
                        Log.d(TAG, "[SAVE] GUARDANDO EN PreferencesManager:");
                        Log.d(TAG, "   - UID: " + uid);
                        Log.d(TAG, "   - DisplayName: " + displayName);
                        Log.d(TAG, "   - Email: " + userEmail);
                        Log.d(TAG, "   - Phone: " + phoneNumber);
                        Log.d(TAG, "   - UserType: " + userType);

                        prefsManager.saveUserData(
                            uid, 
                            displayName != null ? displayName : user.getDisplayName(),
                            userEmail != null ? userEmail : user.getEmail(),
                            phoneNumber != null ? phoneNumber : "",
                            userType
                        );
                        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                        prefsManager.marcarPrimeraVezCompletada();

                        Log.d(TAG, "[OK] Datos guardados. Verificando...");
                        Log.d(TAG, "   - getUserId(): " + prefsManager.getUserId());
                        Log.d(TAG, "   - getUserEmail(): " + prefsManager.getUserEmail());
                        Log.d(TAG, "   - getUserPhone(): " + prefsManager.getUserPhone());

                        // Guardar sesión en Firestore
                        saveSessionToFirestore(uid, userEmail, displayName, userType);

                        // VERIFICAR STATUS DEL GUÍA ANTES DE REDIRIGIR
                        if ("GUIDE".equals(userType)) {
                            checkGuideApprovalStatus(uid);
                        } else {
                            redirigirSegunRol();
                            finish();
                        }
            }

            @Override
            public void onFailure(Exception e) {
                // Si no existe en Firestore, crear usuario en Firestore primero
                Log.w(TAG, "[WARN] Usuario no encontrado en Firestore, creando documento...");
                String userEmail = user.getEmail() != null ? user.getEmail() : "";
                String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";
                
                // Crear usuario en Firestore antes de guardar localmente usando FirestoreManager
                com.example.droidtour.models.User newUser = new com.example.droidtour.models.User();
                newUser.setUserId(uid);
                newUser.setEmail(userEmail);
                newUser.setFirstName(displayName.split(" ")[0]);
                newUser.setLastName(displayName.contains(" ") ? displayName.substring(displayName.indexOf(" ") + 1) : "");
                newUser.setFullName(displayName);
                newUser.setUserType(userType);
                newUser.setActive(true);
                newUser.setProvider("google"); // Asumimos que viene de Google Sign-In
                
                firestoreManager.createUser(newUser, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "[OK] Usuario creado en Firestore");
                        // Ahora guardar localmente con datos básicos
                        prefsManager.saveUserData(uid, displayName, userEmail, "", userType);
                        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                        prefsManager.marcarPrimeraVezCompletada();
                        
                        saveSessionToFirestore(uid, userEmail, displayName, userType);
                        
                        if ("GUIDE".equals(userType)) {
                            checkGuideApprovalStatus(uid);
                        } else {
                            redirigirSegunRol();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "[ERROR] Error creando usuario en Firestore para userId: " + uid, e);
                        // Continuar de todos modos para no bloquear el login
                        prefsManager.saveUserData(uid, displayName, userEmail, "", userType);
                        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                        prefsManager.marcarPrimeraVezCompletada();
                        
                        if ("GUIDE".equals(userType)) {
                            checkGuideApprovalStatus(uid);
                        } else {
                            redirigirSegunRol();
                            finish();
                        }
                    }
                });
            }
        });
    }

    private void checkGuideApprovalStatus(String userId) {
        // Primero revisar si el usuario está activo en users collection usando FirestoreManager
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User userObj = (User) result;
                Boolean isActiveField = userObj.getActive();
                String statusField = userObj.getStatus();

                if ((isActiveField != null && !isActiveField) ||
                        (statusField != null && ("inactive".equalsIgnoreCase(statusField) || "suspended".equalsIgnoreCase(statusField)))) {
                    redirectToUserDisabled(userId, "Tu cuenta ha sido desactivada. Contacta con soporte.");
                    return;
                }

                // Si está activo, proceder a revisar user_roles usando FirestoreManager
                firestoreManager.getUserRoles(userId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rolesData = (Map<String, Object>) result;

                        // Verificar si existe el campo 'guide'
                        if (rolesData.containsKey("guide")) {
                            Map<String, Object> guideData = (Map<String, Object>) rolesData.get("guide");
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
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error al obtener user_roles para userId: " + userId, e);
                        // Error al obtener datos - redirigir a pantalla de espera por seguridad
                        redirectToApprovalPending();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al obtener usuario desde Firestore para userId: " + userId, e);
                // No se pudo leer user doc; por seguridad, redirigir a aprobación pendiente
                redirectToApprovalPending();
            }
        });
    }

    private void redirectToApprovalPending() {
        Intent intent = new Intent(this, GuideApprovalPendingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Redirige a la pantalla que indica que la cuenta está desactivada por el administrador.
     */
    private void redirectToUserDisabled(String userId, String reason) {
        // Sign out current firebase user for safety
        try {
            if (mAuth != null) mAuth.signOut();
        } catch (Exception ignored) {}

        Intent intent = new Intent(this, UserDisabledActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("reason", reason);
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

                                // Obtener TODOS los datos del usuario desde Firestore usando FirestoreManager
                                FirestoreManager firestoreManager = FirestoreManager.getInstance();
                                
                                // Declarar variables fuera del callback para que estén disponibles en ambos métodos
                                final String[] role = {null};
                                final String[] displayName = {"Usuario"};
                                final String[] phoneNumber = {""};
                                
                                firestoreManager.getUserById(uid, new FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        User userObj = (User) result;
                                        
                                        // Obtener rol
                                        role[0] = userObj.getUserType();
                                        if (role[0] == null || role[0].isEmpty()) {
                                            role[0] = inferUserTypeFromEmail(userEmail);
                                        }
                                        
                                        // Obtener nombre completo desde el objeto User
                                        String fullName = userObj.getFullName();
                                        if (fullName != null && !fullName.isEmpty()) {
                                            displayName[0] = fullName;
                                        } else {
                                            String firstName = userObj.getFirstName();
                                            String lastName = userObj.getLastName();
                                            if (firstName != null && lastName != null) {
                                                displayName[0] = firstName + " " + lastName;
                                            }
                                        }
                                        
                                        // Obtener telefono desde el objeto User
                                        String phone = userObj.getPhoneNumber();
                                        phoneNumber[0] = phone != null ? phone : "";
                                        
                                        Log.d(TAG, "[OK] Datos obtenidos de Firestore en login email/password:");
                                        Log.d(TAG, "   - DisplayName: " + displayName[0]);
                                        Log.d(TAG, "   - Phone: " + phoneNumber[0]);
                                        Log.d(TAG, "   - Role: " + role[0]);

                                        prefsManager.saveUserData(uid, displayName[0], userEmail, phoneNumber[0], role[0]);
                                        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                                        prefsManager.marcarPrimeraVezCompletada();

                                        // Guardar sesion en Firestore
                                        saveSessionToFirestore(uid, userEmail, displayName[0], role[0]);

                                        // Redirigir según rol
                                        redirigirSegunRol();
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // No hay documento de perfil, usar inferencia
                                        role[0] = inferUserTypeFromEmail(userEmail);
                                        displayName[0] = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";
                                        phoneNumber[0] = "";
                                        
                                        Log.w(TAG, "[WARN] No se encontro documento en Firestore para login email/password, userId: " + uid + " - " + e.getMessage());
                                        
                                        prefsManager.saveUserData(uid, displayName[0], userEmail, phoneNumber[0], role[0]);
                                        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                                        prefsManager.marcarPrimeraVezCompletada();

                                        // Guardar sesion en Firestore
                                        saveSessionToFirestore(uid, userEmail, displayName[0], role[0]);

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
     * Guardar sesión en Firestore para persistencia multi-dispositivo
     */
    private void saveSessionToFirestore(String userId, String email, String name, String userType) {
        // Obtener información del dispositivo
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        String deviceOS = "Android " + Build.VERSION.RELEASE;
        String appVersion = "1.0.0"; // Puedes obtener esto de BuildConfig.VERSION_NAME
        
        // Crear sesión
        UserSession session = new UserSession(userId, email, name, userType, 
                                            deviceId, deviceModel, deviceOS, appVersion);
        
        // Guardar en Firestore
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.createUserSession(session, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                String sessionId = (String) result;
                // Guardar sessionId en SharedPreferences para futuras referencias
                prefsManager.guardar("session_id", sessionId);
                android.util.Log.d("LoginActivity", "Sesión guardada en Firestore: " + sessionId);
            }
            
            @Override
            public void onFailure(Exception e) {
                // No bloquear el login si falla la sesión en Firestore
                Log.e(TAG, "Error guardando sesión en Firestore para userId: " + userId + " (no crítico)", e);
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
