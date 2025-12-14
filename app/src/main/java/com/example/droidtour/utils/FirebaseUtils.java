package com.example.droidtour.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseUser;
import com.example.droidtour.models.User;
import com.example.droidtour.firebase.FirestoreManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class FirebaseUtils {
    
    private static final String TAG = "FirebaseUtils";
    private static final FirestoreManager firestoreManager = FirestoreManager.getInstance();

    /**
     * Guardar usuario de Google en Firestore usando FirestoreManager
     * @param user Usuario de Firebase Authentication
     * @param userType Tipo de usuario (CLIENT, GUIDE, etc.)
     * @param additionalData Datos adicionales del formulario (firstName, lastName, documentType, etc.)
     */
    public static void saveGoogleUserToFirestore(FirebaseUser user, String userType, Map<String, Object> additionalData) {
        String userId = user.getUid();

        // Crear objeto User usando el modelo
        User newUser = new User();
        newUser.setUserId(userId);
        newUser.setEmail(user.getEmail());
        
        // Dividir displayName en firstName y lastName si es posible
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            newUser.setFullName(displayName);
            String[] nameParts = displayName.split(" ", 2);
            if (nameParts.length > 0) {
                newUser.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    newUser.setLastName(nameParts[1]);
                }
            }
        }
        
        // Usar profileImageUrl
        if (user.getPhotoUrl() != null) {
            newUser.setProfileImageUrl(user.getPhotoUrl().toString());
        }
        
        newUser.setProvider("google");
        newUser.setUserType(userType);
        newUser.setStatus("active");
        newUser.setActive(true);

        // Aplicar datos adicionales del formulario
        if (additionalData != null) {
            if (additionalData.containsKey("firstName")) {
                newUser.setFirstName(String.valueOf(additionalData.get("firstName")));
            }
            if (additionalData.containsKey("lastName")) {
                newUser.setLastName(String.valueOf(additionalData.get("lastName")));
            }
            if (additionalData.containsKey("fullName")) {
                newUser.setFullName(String.valueOf(additionalData.get("fullName")));
            }
            if (additionalData.containsKey("documentType")) {
                newUser.setDocumentType(String.valueOf(additionalData.get("documentType")));
            }
            if (additionalData.containsKey("documentNumber")) {
                newUser.setDocumentNumber(String.valueOf(additionalData.get("documentNumber")));
            }
            if (additionalData.containsKey("birthDate") || additionalData.containsKey("dateOfBirth")) {
                String dateOfBirth = additionalData.containsKey("birthDate") ? 
                    String.valueOf(additionalData.get("birthDate")) : 
                    String.valueOf(additionalData.get("dateOfBirth"));
                newUser.setDateOfBirth(dateOfBirth);
            }
            if (additionalData.containsKey("phone") || additionalData.containsKey("phoneNumber")) {
                String phone = additionalData.containsKey("phone") ? 
                    String.valueOf(additionalData.get("phone")) : 
                    String.valueOf(additionalData.get("phoneNumber"));
                newUser.setPhoneNumber(phone);
            }
            if (additionalData.containsKey("profileCompleted") && additionalData.get("profileCompleted") instanceof Boolean) {
                newUser.setProfileCompleted((Boolean) additionalData.get("profileCompleted"));
            }
            if (additionalData.containsKey("profileCompletedAt") && additionalData.get("profileCompletedAt") instanceof Date) {
                newUser.setProfileCompletedAt((Date) additionalData.get("profileCompletedAt"));
            }
            if (additionalData.containsKey("customPhoto") && additionalData.get("customPhoto") instanceof Boolean) {
                newUser.setCustomPhoto((Boolean) additionalData.get("customPhoto"));
            }
        }

        // Guardar usuario usando FirestoreManager con merge
        firestoreManager.createOrUpdateUser(newUser, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "User saved successfully: " + userId);
                
                // Guardar rol del usuario
                String roleStatus = "CLIENT".equals(userType) ? "active" : "pending";
                firestoreManager.saveUserRole(userId, userType, roleStatus, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "User role saved successfully: " + userType);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error saving user role for userId: " + userId + ", userType: " + userType, e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error saving user", e);
            }
        });
    }

    /**
     * Guardar guía de Google en Firestore usando FirestoreManager
     * @param user Usuario de Firebase Authentication
     * @param userType Tipo de usuario (debe ser "GUIDE")
     * @param additionalData Datos adicionales del formulario
     * @param languages Lista de idiomas que habla el guía
     */
    public static void saveGoogleGuideToFirestore(FirebaseUser user, String userType, Map<String, Object> additionalData, List<String> languages) {
        String userId = user.getUid();

        // Crear objeto User usando el modelo
        User newUser = new User();
        newUser.setUserId(userId);
        newUser.setEmail(user.getEmail());
        
        // Dividir displayName en firstName y lastName si es posible
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            newUser.setFullName(displayName);
            String[] nameParts = displayName.split(" ", 2);
            if (nameParts.length > 0) {
                newUser.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    newUser.setLastName(nameParts[1]);
                }
            }
        }
        
        // Usar profileImageUrl
        if (user.getPhotoUrl() != null) {
            newUser.setProfileImageUrl(user.getPhotoUrl().toString());
        }
        
        newUser.setProvider("google");
        newUser.setUserType(userType);
        newUser.setStatus("pending_approval"); // Estado pendiente para guías
        newUser.setActive(false); // Pendiente de aprobación
        newUser.setGuideApproved(false); // Requiere aprobación

        // Agregar idiomas si existen
        if (languages != null && !languages.isEmpty()) {
            newUser.setGuideLanguages(languages);
        }

        // Aplicar datos adicionales del formulario
        if (additionalData != null) {
            if (additionalData.containsKey("firstName")) {
                newUser.setFirstName(String.valueOf(additionalData.get("firstName")));
            }
            if (additionalData.containsKey("lastName")) {
                newUser.setLastName(String.valueOf(additionalData.get("lastName")));
            }
            if (additionalData.containsKey("fullName")) {
                newUser.setFullName(String.valueOf(additionalData.get("fullName")));
            }
            if (additionalData.containsKey("documentType")) {
                newUser.setDocumentType(String.valueOf(additionalData.get("documentType")));
            }
            if (additionalData.containsKey("documentNumber")) {
                newUser.setDocumentNumber(String.valueOf(additionalData.get("documentNumber")));
            }
            if (additionalData.containsKey("birthDate") || additionalData.containsKey("dateOfBirth")) {
                String dateOfBirth = additionalData.containsKey("birthDate") ? 
                    String.valueOf(additionalData.get("birthDate")) : 
                    String.valueOf(additionalData.get("dateOfBirth"));
                newUser.setDateOfBirth(dateOfBirth);
            }
            if (additionalData.containsKey("phone") || additionalData.containsKey("phoneNumber")) {
                String phone = additionalData.containsKey("phone") ? 
                    String.valueOf(additionalData.get("phone")) : 
                    String.valueOf(additionalData.get("phoneNumber"));
                newUser.setPhoneNumber(phone);
            }
            if (additionalData.containsKey("profileCompleted") && additionalData.get("profileCompleted") instanceof Boolean) {
                newUser.setProfileCompleted((Boolean) additionalData.get("profileCompleted"));
            }
            if (additionalData.containsKey("profileCompletedAt") && additionalData.get("profileCompletedAt") instanceof Date) {
                newUser.setProfileCompletedAt((Date) additionalData.get("profileCompletedAt"));
            }
            if (additionalData.containsKey("customPhoto") && additionalData.get("customPhoto") instanceof Boolean) {
                newUser.setCustomPhoto((Boolean) additionalData.get("customPhoto"));
            }
        }

        // Guardar usuario usando FirestoreManager con merge
        firestoreManager.createOrUpdateUser(newUser, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Guide saved successfully: " + userId);
                
                // Guardar rol del guía (siempre pending para guías)
                firestoreManager.saveUserRole(userId, userType, "pending", new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "Guide role saved successfully");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error saving guide role for userId: " + userId, e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error saving guide", e);
            }
        });
    }

    /**
     * Verificar estado de aprobación del usuario desde user_roles usando FirestoreManager
     * @param userId ID del usuario
     * @param callback Callback para recibir el resultado
     */
    public static void checkUserApprovalStatus(String userId, UserStatusCallback callback) {
        // Usar FirestoreManager para obtener datos de user_roles
        firestoreManager.getUserRoles(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result;
                
                if (data != null && !data.isEmpty()) {
                    // Verificar el estado del usuario
                    Map<String, Object> clientRole = data.containsKey("client") ? 
                        (Map<String, Object>) data.get("client") : null;
                    Map<String, Object> guideRole = data.containsKey("guide") ? 
                        (Map<String, Object>) data.get("guide") : null;

                    if (clientRole != null && "active".equals(clientRole.get("status"))) {
                        callback.onStatusChecked("CLIENT", "active");
                    } else if (guideRole != null) {
                        String guideStatus = (String) guideRole.get("status");
                        callback.onStatusChecked("GUIDE", guideStatus != null ? guideStatus : "pending");
                    } else {
                        callback.onStatusChecked("UNKNOWN", "inactive");
                    }
                } else {
                    callback.onStatusChecked("UNKNOWN", "inactive");
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error checking user approval status for userId: " + userId, e);
                callback.onError(e);
            }
        });
    }

    public interface UserStatusCallback {
        void onStatusChecked(String userType, String status);
        void onError(Exception e);
    }
}