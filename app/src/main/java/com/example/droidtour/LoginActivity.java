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
            String userType = prefsManager.getUserType();
            String userId = prefsManager.getUserId();
            
            // Si es GUIDE, validar estado de aprobación antes de redirigir
            if ("GUIDE".equals(userType) && userId != null && !userId.isEmpty()) {
                checkGuideApprovalStatusOnRestore(userId);
            } else {
                redirigirSegunRol();
                finish();
            }
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

        // Obtener datos completos desde Firestore
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserById(uid, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User userObj = (User) result;

                // Revisar si la cuenta está desactivada
                String statusField = userObj.getStatus();
                if (statusField != null && ("inactive".equalsIgnoreCase(statusField) ||
                        "suspended".equalsIgnoreCase(statusField))) {
                    redirectToUserDisabled(uid, "Tu cuenta ha sido desactivada. Contacta con soporte.");
                    return;
                }

                // Extraer datos del usuario
                String fullName = extractFullName(userObj);
                String userEmail = userObj.getEmail();
                String phoneNumber = extractPhoneNumber(userObj);

                // Guardar datos en PreferencesManager
                Log.d(TAG, "[SAVE] GUARDANDO EN PreferencesManager:");
                Log.d(TAG, "   - UID: " + uid);
                Log.d(TAG, "   - DisplayName: " + fullName);
                Log.d(TAG, "   - Email: " + userEmail);
                Log.d(TAG, "   - Phone: " + phoneNumber);
                Log.d(TAG, "   - UserType: " + userType);

                prefsManager.saveUserData(
                        uid,
                        fullName != null ? fullName : user.getDisplayName(),
                        userEmail != null ? userEmail : user.getEmail(),
                        phoneNumber != null ? phoneNumber : "",
                        userType
                );
                prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                prefsManager.marcarPrimeraVezCompletada();

                // Guardar sesión en Firestore
                saveSessionToFirestore(uid, userEmail, fullName, userType);

                // Verificar status del guía antes de redirigir
                if ("GUIDE".equals(userType)) {
                    checkGuideApprovalStatus(uid);
                } else {
                    redirigirSegunRol();
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Si no existe en Firestore, crear usuario básico
                Log.w(TAG, "[WARN] Usuario no encontrado en Firestore, creando documento...");
                createUserInFirestore(user, userType, uid);
            }
        });
    }

    /**
     * Extraer nombre completo del usuario
     */
    private String extractFullName(User userObj) {
        if (userObj.getPersonalData() != null) {
            String fullName = userObj.getPersonalData().getFullName();
            if (fullName != null && !fullName.isEmpty()) {
                return fullName;
            }

            String firstName = userObj.getPersonalData().getFirstName();
            String lastName = userObj.getPersonalData().getLastName();
            if (firstName != null || lastName != null) {
                return ((firstName != null ? firstName : "") + " " +
                        (lastName != null ? lastName : "")).trim();
            }
        }
        return "Usuario";
    }

    /**
     * Extraer teléfono del usuario
     */
    private String extractPhoneNumber(User userObj) {
        if (userObj.getPersonalData() != null) {
            return userObj.getPersonalData().getPhoneNumber();
        }
        return "";
    }

    /**
     * Crear usuario en Firestore
     */
    private void createUserInFirestore(FirebaseUser user, String userType, String uid) {
        String userEmail = user.getEmail() != null ? user.getEmail() : "";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";

        // Crear usuario básico
        User newUser = new User();
        newUser.setUserId(uid);
        newUser.setEmail(userEmail);
        newUser.setUserType(userType);
        newUser.setStatus("active");

        // Crear PersonalData si viene de Google
        User.PersonalData personalData = new User.PersonalData();
        String[] nameParts = displayName.split(" ", 2);
        personalData.setFirstName(nameParts[0]);
        personalData.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        personalData.setFullName(displayName);

        // Agregar foto si existe
        if (user.getPhotoUrl() != null) {
            personalData.setProfileImageUrl(user.getPhotoUrl().toString());
        }

        newUser.setPersonalData(personalData);

        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.createUser(newUser, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "[OK] Usuario creado en Firestore");
                // Guardar localmente
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
                Log.e(TAG, "[ERROR] Error creando usuario en Firestore", e);
                // Continuar de todos modos
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

    private void checkGuideApprovalStatus(String userId) {
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User userObj = (User) result;
                String statusField = userObj.getStatus();

                if (statusField != null && ("inactive".equalsIgnoreCase(statusField) ||
                        "suspended".equalsIgnoreCase(statusField))) {
                    redirectToUserDisabled(userId, "Tu cuenta ha sido desactivada. Contacta con soporte.");
                    return;
                }

                // Revisar user_roles
                firestoreManager.getUserRoles(userId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rolesData = (Map<String, Object>) result;

                        String guideStatus = extractGuideStatus(rolesData);

                        if ("active".equals(guideStatus)) {
                            // Guía aprobado
                            redirigirSegunRol();
                            finish();
                        } else {
                            // Guía no aprobado
                            redirectToApprovalPending();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error al obtener user_roles", e);
                        redirectToApprovalPending();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al obtener usuario", e);
                redirectToApprovalPending();
            }
        });
    }

    /**
     * Extraer estado de guía desde diferentes estructuras posibles
     */
    private String extractGuideStatus(Map<String, Object> rolesData) {
        // Estructura 1: directa
        if (rolesData.containsKey("status")) {
            return (String) rolesData.get("status");
        }

        // Estructura 2: bajo "guide"
        if (rolesData.containsKey("guide")) {
            Object guideObj = rolesData.get("guide");
            if (guideObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> guideMap = (Map<String, Object>) guideObj;
                return (String) guideMap.get("status");
            }
        }

        // Estructura 3: bajo "roles.guide"
        if (rolesData.containsKey("roles")) {
            Object rolesObj = rolesData.get("roles");
            if (rolesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rolesMap = (Map<String, Object>) rolesObj;
                Object guideRole = rolesMap.get("guide");
                if (guideRole instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> guideMap = (Map<String, Object>) guideRole;
                    return (String) guideMap.get("status");
                }
            }
        }

        return null;
    }

    private void redirectToApprovalPending() {
        Intent intent = new Intent(this, GuideApprovalPendingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Validar estado de aprobación del guía al restaurar sesión
     */
    private void checkGuideApprovalStatusOnRestore(String userId) {
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User userObj = (User) result;
                String statusField = userObj.getStatus();

                if (statusField != null && ("inactive".equalsIgnoreCase(statusField) ||
                        "suspended".equalsIgnoreCase(statusField))) {
                    redirectToUserDisabled(userId, "Tu cuenta ha sido desactivada. Contacta con soporte.");
                    return;
                }

                // Revisar user_roles para verificar estado de aprobación
                firestoreManager.getUserRoles(userId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rolesData = (Map<String, Object>) result;

                        String guideStatus = extractGuideStatus(rolesData);

                        if ("active".equals(guideStatus)) {
                            // Guía aprobado - redirigir al dashboard
                            redirigirSegunRol();
                            finish();
                        } else {
                            // Guía no aprobado - redirigir a pantalla de espera
                            redirectToApprovalPending();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error al obtener user_roles al restaurar sesión", e);
                        // Por seguridad, redirigir a pantalla de espera si no se puede verificar
                        redirectToApprovalPending();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al obtener usuario al restaurar sesión", e);
                // Por seguridad, redirigir a pantalla de espera si no se puede verificar
                redirectToApprovalPending();
            }
        });
    }

    private void redirectToUserDisabled(String userId, String reason) {
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
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                handleEmailPasswordLogin(user, email);
                            } else {
                                Toast.makeText(LoginActivity.this, "Error al obtener usuario", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String message = task.getException() != null ? task.getException().getMessage() : "Autenticación fallida";
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Manejar login con email/password
     */
    private void handleEmailPasswordLogin(FirebaseUser user, String email) {
        String uid = user.getUid();

        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserById(uid, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User userObj = (User) result;

                // Revisar si la cuenta está desactivada
                String statusField = userObj.getStatus();
                if (statusField != null && ("inactive".equalsIgnoreCase(statusField) ||
                        "suspended".equalsIgnoreCase(statusField))) {
                    redirectToUserDisabled(uid, "Tu cuenta ha sido desactivada. Contacta con soporte.");
                    return;
                }

                String role = userObj.getUserType();
                if (role == null || role.isEmpty()) {
                    role = inferUserTypeFromEmail(email);
                }

                String displayName = extractFullName(userObj);
                String phoneNumber = extractPhoneNumber(userObj);

                Log.d(TAG, "[OK] Datos obtenidos de Firestore en login email/password:");
                Log.d(TAG, "   - DisplayName: " + displayName);
                Log.d(TAG, "   - Phone: " + phoneNumber);
                Log.d(TAG, "   - Role: " + role);

                prefsManager.saveUserData(uid, displayName, email, phoneNumber, role);
                prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                prefsManager.marcarPrimeraVezCompletada();

                saveSessionToFirestore(uid, email, displayName, role);

                // Verificar status del guía antes de redirigir (igual que en handleExistingUser)
                if ("GUIDE".equals(role)) {
                    checkGuideApprovalStatus(uid);
                } else {
                    redirigirSegunRol();
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Usar inferencia como fallback
                String role = inferUserTypeFromEmail(email);
                String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";

                Log.w(TAG, "[WARN] No se encontró documento en Firestore para login email/password", e);

                prefsManager.saveUserData(uid, displayName, email, "", role);
                prefsManager.guardarUltimoLogin(System.currentTimeMillis());
                prefsManager.marcarPrimeraVezCompletada();

                saveSessionToFirestore(uid, email, displayName, role);

                // Si no existe en Firestore, no puede ser un guía registrado, así que redirigir normalmente
                // (un guía registrado siempre debería existir en Firestore)
                redirigirSegunRol();
                finish();
            }
        });
    }

    /**
     * Guardar sesión en Firestore
     */
    private void saveSessionToFirestore(String userId, String email, String name, String userType) {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        String deviceOS = "Android " + Build.VERSION.RELEASE;
        String appVersion = "1.0.0";

        UserSession session = new UserSession(userId, email, name, userType,
                deviceId, deviceModel, deviceOS, appVersion);

        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.createUserSession(session, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                UserSession savedSession = (UserSession) result;
                prefsManager.guardar("session_id", savedSession.getSessionId());
                Log.d(TAG, "Sesión guardada en Firestore: " + savedSession.getSessionId());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error guardando sesión en Firestore (no crítico)", e);
            }
        });
    }

    private String inferUserTypeFromEmail(String email) {
        if (email == null) return "CLIENT";
        String lower = email.toLowerCase();
        if (lower.contains("superadmin@droidtour.com") || lower.contains("superadmin")) return "SUPERADMIN";
        if (lower.contains("admin@tours.com") || lower.contains("admin")) return "ADMIN";
        if (lower.contains("guia@tours.com") || lower.contains("guia") || lower.contains("guide")) return "GUIDE";
        return "CLIENT";
    }

    private void redirigirSegunRol(){
        String tipoUsuario = prefsManager.getUserType();
        Intent intent;

        switch (tipoUsuario){
            case "SUPERADMIN":
                intent = new Intent(this, SuperadminMainActivity.class);
                break;
            case "COMPANY_ADMIN":
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