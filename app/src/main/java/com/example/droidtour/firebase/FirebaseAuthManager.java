package com.example.droidtour.firebase;

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
        if (instance == null) instance = new FirebaseAuthManager(context);
        return instance;
    }

    // Devuelve el UID del usuario actualmente autenticado, o null
    public String getCurrentUserId() {
        FirebaseUser u = mAuth.getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    // ==================== REGISTRO Y LOGIN CON EMAIL ====================

    public void registerWithEmail(String email, String password, User userData, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        callback.onFailure(task.getException());
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onFailure(new Exception("FirebaseUser is null after registration"));
                        return;
                    }

                    // Guardar perfil en Firestore (enhanced)
                    userData.setUserId(firebaseUser.getUid());
                    userData.setEmail(email);

                    FirestoreManager firestoreManager = FirestoreManager.getInstance();
                    firestoreManager.upsertUser(userData, new FirestoreManager.FirestoreCallback() {
                        @Override public void onSuccess(Object result) {
                            sendEmailVerification(firebaseUser);
                            callback.onSuccess(firebaseUser);
                        }

                        @Override public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
                });
    }

    public void loginWithEmail(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) callback.onSuccess(mAuth.getCurrentUser());
                    else callback.onFailure(task.getException());
                });
    }

    // ==================== GOOGLE SIGN-IN ====================

    public void setupGoogleSignIn(String webClientId) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public GoogleSignInClient getGoogleSignInClient() {
        return mGoogleSignInClient;
    }

    public void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask, User defaultUserData, AuthCallback callback) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) firebaseAuthWithGoogle(account.getIdToken(), defaultUserData, callback);
            else callback.onFailure(new Exception("Google account is null"));
        } catch (ApiException e) {
            callback.onFailure(e);
        }
    }

    private void firebaseAuthWithGoogle(String idToken, User defaultUserData, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailure(task.getException());
                return;
            }

            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            if (firebaseUser == null) {
                callback.onFailure(new Exception("FirebaseUser is null after Google sign-in"));
                return;
            }

            FirestoreManager firestoreManager = FirestoreManager.getInstance();
            firestoreManager.getUserById(firebaseUser.getUid(), new FirestoreManager.FirestoreCallback() {
                @Override public void onSuccess(Object result) {
                    // Ya existe perfil
                    callback.onSuccess(firebaseUser);
                }

                @Override public void onFailure(Exception e) {
                    // No existe: crear perfil mínimo
                    if (defaultUserData == null) {
                        callback.onFailure(new Exception("No default profile data provided"));
                        return;
                    }

                    defaultUserData.setUserId(firebaseUser.getUid());
                    defaultUserData.setEmail(firebaseUser.getEmail());

                    // Si quieres auto-completar nombres desde Google:
                    if (defaultUserData.getPersonalData() != null && firebaseUser.getDisplayName() != null) {
                        String display = firebaseUser.getDisplayName().trim();
                        String[] parts = display.split("\\s+");
                        defaultUserData.getPersonalData().setFirstName(parts.length > 0 ? parts[0] : "");
                        defaultUserData.getPersonalData().setLastName(parts.length > 1 ? parts[parts.length - 1] : "");
                        defaultUserData.getPersonalData().setFullName(display);
                    }

                    if (defaultUserData.getPersonalData() != null && firebaseUser.getPhotoUrl() != null) {
                        defaultUserData.getPersonalData().setProfileImageUrl(firebaseUser.getPhotoUrl().toString());
                    }

                    firestoreManager.upsertUser(defaultUserData, new FirestoreManager.FirestoreCallback() {
                        @Override public void onSuccess(Object result) {
                            callback.onSuccess(firebaseUser);
                        }

                        @Override public void onFailure(Exception ex) {
                            callback.onFailure(ex);
                        }
                    });
                }
            });
        });
    }

    // ==================== EMAIL VERIFICATION ====================

    private void sendEmailVerification(@NonNull FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) Log.d(TAG, "Verification email sent.");
            else Log.w(TAG, "Verification email failed", task.getException());
        });
    }

    // ==================== CALLBACKS ====================

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
}
