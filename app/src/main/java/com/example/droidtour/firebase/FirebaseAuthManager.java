package com.example.droidtour.firebase;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.droidtour.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Manager para Firebase Authentication
 * Proporciona métodos para autenticación de usuarios (email/password, Google)
 */
public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private static FirebaseAuthManager instance;
    
    private final FirebaseAuth mAuth;
    private final Context context;
    private GoogleSignInClient mGoogleSignInClient;

    private FirebaseAuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthManager(context);
        }
        return instance;
    }

    // ==================== REGISTRO Y LOGIN CON EMAIL ====================

    /**
     * Registrar un nuevo usuario con email y contraseña
     */
    public void registerWithEmail(String email, String password, User userData, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Guardar datos adicionales en Firestore
                            userData.setUserId(firebaseUser.getUid());
                            userData.setEmail(email);
                            
                            FirestoreManager firestoreManager = FirestoreManager.getInstance();
                            firestoreManager.createUser(userData, new FirestoreManager.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    // Enviar email de verificación
                                    sendEmailVerification(firebaseUser);
                                    callback.onSuccess(firebaseUser);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    callback.onFailure(e);
                                }
                            });
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    /**
     * Iniciar sesión con email y contraseña
     */
    public void loginWithEmail(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ==================== AUTENTICACIÓN CON GOOGLE ====================

    /**
     * Configurar Google Sign-In
     * Llamar este método en onCreate de tu Activity
     */
    public void setupGoogleSignIn(String webClientId) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId) // Obtenido de google-services.json
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    /**
     * Obtener GoogleSignInClient para iniciar el flujo de login
     */
    public GoogleSignInClient getGoogleSignInClient() {
        return mGoogleSignInClient;
    }

    /**
     * Procesar el resultado de Google Sign-In
     * Llamar en onActivityResult de tu Activity
     */
    public void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask, User defaultUserData, AuthCallback callback) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken(), defaultUserData, callback);
            }
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            callback.onFailure(e);
        }
    }

    private void firebaseAuthWithGoogle(String idToken, User defaultUserData, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        
                        if (firebaseUser != null) {
                            // Verificar si el usuario ya existe en Firestore
                            FirestoreManager firestoreManager = FirestoreManager.getInstance();
                            firestoreManager.getUserById(firebaseUser.getUid(), new FirestoreManager.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    // Usuario ya existe
                                    callback.onSuccess(firebaseUser);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    // Usuario no existe, crear perfil
                                    if (defaultUserData != null) {
                                        defaultUserData.setUserId(firebaseUser.getUid());
                                        defaultUserData.setEmail(firebaseUser.getEmail());
                                        
                                        // Extraer nombre de Google
                                        if (firebaseUser.getDisplayName() != null) {
                                            String[] nameParts = firebaseUser.getDisplayName().split(" ");
                                            defaultUserData.setFirstName(nameParts[0]);
                                            if (nameParts.length > 1) {
                                                defaultUserData.setLastName(nameParts[nameParts.length - 1]);
                                            }
                                            defaultUserData.setFullName(firebaseUser.getDisplayName());
                                        }
                                        
                                        // URL de foto de perfil de Google
                                        if (firebaseUser.getPhotoUrl() != null) {
                                            defaultUserData.setProfileImageUrl(firebaseUser.getPhotoUrl().toString());
                                        }
                                        
                                        firestoreManager.createUser(defaultUserData, new FirestoreManager.FirestoreCallback() {
                                            @Override
                                            public void onSuccess(Object result) {
                                                callback.onSuccess(firebaseUser);
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                callback.onFailure(e);
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ==================== RECUPERACIÓN DE CONTRASEÑA ====================

    /**
     * Enviar email de recuperación de contraseña
     */
    public void sendPasswordResetEmail(String email, SimpleCallback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent.");
                        callback.onSuccess();
                    } else {
                        Log.w(TAG, "Password reset email failed", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ==================== VERIFICACIÓN DE EMAIL ====================

    /**
     * Enviar email de verificación al usuario actual
     */
    public void sendEmailVerification(FirebaseUser user) {
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Verification email sent to " + user.getEmail());
                        }
                    });
        }
    }

    /**
     * Verificar si el email del usuario actual está verificado
     */
    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    // ==================== GESTIÓN DE SESIÓN ====================

    /**
     * Cerrar sesión del usuario actual
     */
    public void logout(SimpleCallback callback) {
        try {
            mAuth.signOut();
            if (mGoogleSignInClient != null) {
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    Log.d(TAG, "User logged out");
                    callback.onSuccess();
                });
            } else {
                callback.onSuccess();
            }
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    /**
     * Obtener usuario actual de Firebase
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Verificar si hay un usuario autenticado
     */
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Obtener ID del usuario actual
     */
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Obtener email del usuario actual
     */
    public String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    // ==================== CALLBACKS ====================

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}

