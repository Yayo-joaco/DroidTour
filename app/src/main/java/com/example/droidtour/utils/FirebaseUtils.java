package com.example.droidtour.utils;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class FirebaseUtils {

    public static void saveGoogleUserToFirestore(FirebaseUser user, String userType, Map<String, Object> additionalData) {
        String userId = user.getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userId);
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("photoURL", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        userData.put("provider", "google");
        userData.put("userType", userType);
        userData.put("createdAt", new Date());
        userData.put("status", "active");

        // Agregar datos adicionales específicos del formulario
        if (additionalData != null) {
            userData.putAll(additionalData);
        }

        // Guardar en users collection
        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .set(userData, SetOptions.merge()) // Usar merge para no sobreescribir datos existentes
                .addOnSuccessListener(aVoid -> {
                    // Para CLIENTE, no necesitamos aprobación, así que activamos directamente
                    if ("CLIENT".equals(userType)) {
                        saveUserRole(userId, userType, "active");
                    } else if ("GUIDE".equals(userType)) {
                        saveUserRole(userId, userType, "pending");
                    }
                })
                .addOnFailureListener(e -> {
                    // Manejar error
                });
    }

    private static void saveUserRole(String userId, String userType, String status) {
        Map<String, Object> roleData = new HashMap<>();
        roleData.put("status", status);
        roleData.put("updatedAt", new Date());

        if ("active".equals(status)) {
            roleData.put("activatedAt", new Date());
        } else if ("pending".equals(status)) {
            roleData.put("appliedAt", new Date());
        }

        // Crear el mapa completo de roles
        Map<String, Object> roleUpdate = new HashMap<>();
        roleUpdate.put(userType.toLowerCase(), roleData);

        FirebaseFirestore.getInstance().collection("user_roles")
                .document(userId)
                .set(roleUpdate, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Éxito
                })
                .addOnFailureListener(e -> {
                    // Manejar error
                });
    }


    public static void saveGoogleGuideToFirestore(FirebaseUser user, String userType, Map<String, Object> additionalData, List<String> languages) {
        String userId = user.getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userId);
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("photoURL", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        userData.put("provider", "google");
        userData.put("userType", userType);
        userData.put("createdAt", new Date());
        userData.put("status", "pending_approval"); // Estado pendiente para guías

        // Agregar datos adicionales específicos del formulario
        if (additionalData != null) {
            userData.putAll(additionalData);
        }

        // Agregar idiomas si existen
        if (languages != null && !languages.isEmpty()) {
            userData.put("languages", languages);
        }

        // Guardar en users collection
        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Para GUÍA, necesita aprobación
                    saveUserRole(userId, userType, "pending");
                })
                .addOnFailureListener(e -> {
                    // Manejar error
                });
    }

    public static void checkUserApprovalStatus(String userId, UserStatusCallback callback) {
        FirebaseFirestore.getInstance().collection("user_roles")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> roles = (Map<String, Object>) documentSnapshot.get("roles");
                        if (roles != null) {
                            // Verificar el estado del usuario
                            Map<String, Object> clientRole = (Map<String, Object>) roles.get("client");
                            Map<String, Object> guideRole = (Map<String, Object>) roles.get("guide");

                            if (clientRole != null && "active".equals(clientRole.get("status"))) {
                                callback.onStatusChecked("CLIENT", "active");
                            } else if (guideRole != null) {
                                String guideStatus = (String) guideRole.get("status");
                                callback.onStatusChecked("GUIDE", guideStatus);
                            } else {
                                callback.onStatusChecked("UNKNOWN", "inactive");
                            }
                        } else {
                            callback.onStatusChecked("UNKNOWN", "inactive");
                        }
                    } else {
                        callback.onStatusChecked("UNKNOWN", "inactive");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(e);
                });
    }

    public interface UserStatusCallback {
        void onStatusChecked(String userType, String status);
        void onError(Exception e);
    }
}