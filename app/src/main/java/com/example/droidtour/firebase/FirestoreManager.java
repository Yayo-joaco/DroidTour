package com.example.droidtour.firebase;

import android.util.Log;
import android.graphics.Bitmap;
import android.net.Uri;

import com.example.droidtour.models.Company;
import com.example.droidtour.models.Guide;
import com.example.droidtour.models.Message;
import com.example.droidtour.models.User;
import com.example.droidtour.models.TourOffer;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.models.Notification;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.PaymentMethod;
import com.example.droidtour.models.Review;
import com.example.droidtour.models.UserSession;
import com.example.droidtour.utils.ImageUploadManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;

    private final FirebaseFirestore db;

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_COMPANIES = "companies";
    public static final String COLLECTION_GUIDES = "guides";

    // Nuevas colecciones usadas en la app
    public static final String COLLECTION_TOUR_OFFERS = "tour_offers";
    public static final String COLLECTION_RESERVATIONS = "reservations";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_TOURS = "tours";
    public static final String COLLECTION_SERVICES = "services";

    // Colecciones adicionales necesarias
    public static final String COLLECTION_PAYMENT_METHODS = "payment_methods";
    public static final String COLLECTION_USER_ROLES = "user_roles";
    public static final String COLLECTION_USER_SESSIONS = "user_sessions";
    public static final String COLLECTION_REVIEWS = "reviews";
    public static final String COLLECTION_MESSAGES = "messages";

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) instance = new FirestoreManager();
        return instance;
    }

    // ==================== USERS ====================

    /**
     * Obtener todos los usuarios de tipo GUIDE
     */
    public void getGuideUsers(FirestoreCallback callback) {
        db.collection(COLLECTION_USERS)
                .whereEqualTo("guide", true)
                .get()
                .addOnSuccessListener(qs -> {
                    List<User> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            if (u.getUserId() == null || u.getUserId().isEmpty()) {
                                u.setUserId(doc.getId());
                            }
                            list.add(u);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void upsertUser(User user, FirestoreCallback callback) {
        String err = validateUser(user);
        if (err != null) {
            callback.onFailure(new Exception(err));
            return;
        }

        // Asegurar fullName coherente si existe personalData
        if (user.getPersonalData() != null) {
            User.PersonalData pd = user.getPersonalData();
            if (pd.getFirstName() != null && pd.getLastName() != null) {
                pd.setFullName((pd.getFirstName() + " " + pd.getLastName()).trim());
            }
        }

        db.collection(COLLECTION_USERS)
                .document(user.getUserId())
                .set(user, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(user))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Registrar un cliente (nuevo o existente): valida campos mínimos, asigna userId si hace falta,
     * sube imagen de perfil (Bitmap o Uri) y guarda la URL en el documento antes de persistir el User.
     *
     * Nota: este método NO usa validateUser() porque ese método exige userId; aquí generamos uno si hace falta.
     */
    public void registerClient(User user, Uri profileImageUri, Bitmap profileBitmap, FirestoreCallback callback) {
        if (user == null) {
            callback.onFailure(new Exception("User is null"));
            return;
        }

        // Forzar tipo CLIENT
        user.setUserType("CLIENT");

        // Validaciones mínimas
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            callback.onFailure(new Exception("Email is required"));
            return;
        }
        if (user.getPersonalData() == null) {
            callback.onFailure(new Exception("personalData is required for CLIENT"));
            return;
        }
        User.PersonalData pd = user.getPersonalData();
        if (pd.getFirstName() == null || pd.getFirstName().trim().isEmpty()) {
            callback.onFailure(new Exception("First name is required"));
            return;
        }
        if (pd.getLastName() == null || pd.getLastName().trim().isEmpty()) {
            callback.onFailure(new Exception("Last name is required"));
            return;
        }
        if (pd.getDocumentType() == null || pd.getDocumentType().trim().isEmpty()) {
            callback.onFailure(new Exception("Document type is required"));
            return;
        }
        if (pd.getDocumentNumber() == null || pd.getDocumentNumber().trim().isEmpty()) {
            callback.onFailure(new Exception("Document number is required"));
            return;
        }

        // Crear o asegurar userId
        DocumentReference userRef = (user.getUserId() == null || user.getUserId().trim().isEmpty())
                ? db.collection(COLLECTION_USERS).document()
                : db.collection(COLLECTION_USERS).document(user.getUserId());
        user.setUserId(userRef.getId());

        // Asegurar fullName
        if (pd.getFirstName() != null && pd.getLastName() != null) {
            pd.setFullName((pd.getFirstName() + " " + pd.getLastName()).trim());
        }

        // Helper para guardar el usuario final
        final Runnable saveUser = () -> userRef.set(user, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(user))
                .addOnFailureListener(callback::onFailure);

        // Si se proporciona Bitmap
        if (profileBitmap != null) {
            UploadTask upload = ImageUploadManager.uploadUserProfileImage(user.getUserId(), profileBitmap);
            if (upload == null) {
                callback.onFailure(new Exception("Failed to start image upload"));
                return;
            }

            ImageUploadManager.getDownloadUrl(upload, new ImageUploadManager.ImageUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // Actualizar el objeto User con la URL de la imagen en personalData
                    if (user.getPersonalData() != null) {
                        user.getPersonalData().setProfileImageUrl(downloadUrl);
                    } else {
                        // Si no existe personalData, crearlo
                        User.PersonalData pd = new User.PersonalData();
                        pd.setProfileImageUrl(downloadUrl);
                        user.setPersonalData(pd);
                    }
                    // Guardar el usuario completo con la URL actualizada
                    saveUser.run();
                }

                @Override
                public void onFailure(Exception exception) {
                    callback.onFailure(exception);
                }
            });
            return;
        }

        // Si se proporciona Uri
        if (profileImageUri != null) {
            UploadTask upload = ImageUploadManager.uploadFromUri(profileImageUri, "profile_images", user.getUserId());
            if (upload == null) {
                callback.onFailure(new Exception("Failed to start image upload from Uri"));
                return;
            }

            ImageUploadManager.getDownloadUrl(upload, new ImageUploadManager.ImageUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // Actualizar el objeto User con la URL de la imagen en personalData
                    if (user.getPersonalData() != null) {
                        user.getPersonalData().setProfileImageUrl(downloadUrl);
                    } else {
                        // Si no existe personalData, crearlo
                        User.PersonalData pd = new User.PersonalData();
                        pd.setProfileImageUrl(downloadUrl);
                        user.setPersonalData(pd);
                    }
                    // Guardar el usuario completo con la URL actualizada
                    saveUser.run();
                }

                @Override
                public void onFailure(Exception exception) {
                    callback.onFailure(exception);
                }
            });
            return;
        }

        // Sin imagen: guardar usuario directamente
        saveUser.run();
    }

    public void getUserById(String userId, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new Exception("User ID is required"));
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure(new Exception("User not found"));
                        return;
                    }
                    User user = doc.toObject(User.class);
                    if (user != null && user.getUserId() == null) user.setUserId(doc.getId());
                    callback.onSuccess(user);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Elimina un usuario de Firestore y TODOS sus datos relacionados
     * Elimina: 
     * - Reservaciones (userId, guideId)
     * - Notificaciones (userId)
     * - Reseñas (userId, guideId)
     * - Mensajes (senderId, receiverId)
     * - Métodos de pago (userId)
     * - Sesiones de usuario (userId)
     * - Tour offers (guideId si es GUIDE)
     * - guides/{userId} (si es GUIDE)
     * - user_roles/{userId}
     * - users/{userId}
     */
    public void deleteUser(String userId, String userType, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new Exception("User ID is required"));
            return;
        }

        Log.d(TAG, "Iniciando eliminación completa del usuario: " + userId);

        // Eliminar primero todos los datos relacionados, luego el usuario
        deleteUserRelatedData(userId, userType, new FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Después de eliminar datos relacionados, eliminar el usuario principal
                deleteUserMainData(userId, userType, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error eliminando datos relacionados del usuario, continuando...", e);
                // Continuar con eliminación del usuario principal aunque haya errores en datos relacionados
                deleteUserMainData(userId, userType, callback);
            }
        });
    }

    /**
     * Elimina todos los datos relacionados con un usuario
     */
    private void deleteUserRelatedData(String userId, String userType, FirestoreCallback callback) {
        final int[] completedTasks = {0};
        final int totalTasks = 7; // Número de tareas de eliminación
        final boolean[] hasError = {false};

        FirestoreCallback taskCallback = new FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                synchronized (completedTasks) {
                    completedTasks[0]++;
                    if (completedTasks[0] >= totalTasks) {
                        if (hasError[0]) {
                            Log.w(TAG, "Algunas eliminaciones de datos relacionados fallaron, pero se continuará");
                        }
                        callback.onSuccess(true);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                synchronized (completedTasks) {
                    hasError[0] = true;
                    completedTasks[0]++;
                    Log.w(TAG, "Error en una tarea de eliminación: " + e.getMessage());
                    if (completedTasks[0] >= totalTasks) {
                        callback.onSuccess(true); // Continuar aunque haya errores
                    }
                }
            }
        };

        // 1. Eliminar reservaciones
        deleteUserReservations(userId, userType, taskCallback);

        // 2. Eliminar notificaciones
        deleteUserNotifications(userId, taskCallback);

        // 3. Eliminar reseñas
        deleteUserReviews(userId, userType, taskCallback);

        // 4. Eliminar mensajes
        deleteUserMessages(userId, taskCallback);

        // 5. Eliminar métodos de pago
        deleteUserPaymentMethods(userId, taskCallback);

        // 6. Eliminar sesiones de usuario
        deleteUserSessions(userId, taskCallback);

        // 7. Eliminar tour offers (si es guía)
        if ("GUIDE".equals(userType)) {
            deleteUserTourOffers(userId, taskCallback);
        } else {
            // Si no es guía, contar esta tarea como completada
            taskCallback.onSuccess(true);
        }
    }

    /**
     * Elimina las reservaciones del usuario (como cliente o guía)
     */
    private void deleteUserReservations(String userId, String userType, FirestoreCallback callback) {
        // Buscar reservaciones donde el usuario es cliente (userId) o guía (guideId)
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentReference> toDelete = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        toDelete.add(doc.getReference());
                    }
                    
                    // Si es guía, también buscar por guideId
                    if ("GUIDE".equals(userType)) {
                        db.collection(COLLECTION_RESERVATIONS)
                                .whereEqualTo("guideId", userId)
                                .get()
                                .addOnSuccessListener(guideQuerySnapshot -> {
                                    for (QueryDocumentSnapshot doc : guideQuerySnapshot) {
                                        toDelete.add(doc.getReference());
                                    }
                                    deleteDocuments(toDelete, "reservaciones", callback);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Error buscando reservaciones por guideId", e);
                                    deleteDocuments(toDelete, "reservaciones", callback);
                                });
                    } else {
                        deleteDocuments(toDelete, "reservaciones", callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error buscando reservaciones", e);
                    callback.onSuccess(true); // Continuar aunque falle
                });
    }

    /**
     * Elimina las notificaciones del usuario
     */
    private void deleteUserNotifications(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentReference> toDelete = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        toDelete.add(doc.getReference());
                    }
                    deleteDocuments(toDelete, "notificaciones", callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error buscando notificaciones", e);
                    callback.onSuccess(true);
                });
    }

    /**
     * Elimina las reseñas del usuario (como cliente o guía)
     */
    private void deleteUserReviews(String userId, String userType, FirestoreCallback callback) {
        // Buscar reseñas donde el usuario es el autor (userId)
        db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentReference> toDelete = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        toDelete.add(doc.getReference());
                    }
                    
                    // Nota: Las reseñas también pueden tener guideId, pero generalmente se busca por userId
                    deleteDocuments(toDelete, "reseñas", callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error buscando reseñas", e);
                    callback.onSuccess(true);
                });
    }

    /**
     * Elimina los mensajes donde el usuario es remitente o receptor
     */
    private void deleteUserMessages(String userId, FirestoreCallback callback) {
        // Buscar mensajes como sender
        db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("senderId", userId)
                .get()
                .addOnSuccessListener(senderQuerySnapshot -> {
                    List<DocumentReference> toDelete = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : senderQuerySnapshot) {
                        toDelete.add(doc.getReference());
                    }
                    
                    // Buscar mensajes como receiver
                    db.collection(COLLECTION_MESSAGES)
                            .whereEqualTo("receiverId", userId)
                            .get()
                            .addOnSuccessListener(receiverQuerySnapshot -> {
                                for (QueryDocumentSnapshot doc : receiverQuerySnapshot) {
                                    toDelete.add(doc.getReference());
                                }
                                deleteDocuments(toDelete, "mensajes", callback);
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error buscando mensajes como receiver", e);
                                deleteDocuments(toDelete, "mensajes", callback);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error buscando mensajes como sender", e);
                    callback.onSuccess(true);
                });
    }

    /**
     * Elimina los métodos de pago del usuario
     */
    private void deleteUserPaymentMethods(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_PAYMENT_METHODS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentReference> toDelete = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        toDelete.add(doc.getReference());
                    }
                    deleteDocuments(toDelete, "métodos de pago", callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error buscando métodos de pago", e);
                    callback.onSuccess(true);
                });
    }

    /**
     * Elimina las sesiones del usuario
     */
    private void deleteUserSessions(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_SESSIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentReference> toDelete = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        toDelete.add(doc.getReference());
                    }
                    deleteDocuments(toDelete, "sesiones de usuario", callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error buscando sesiones de usuario", e);
                    callback.onSuccess(true);
                });
    }

    /**
     * Elimina las ofertas de tours del guía
     */
    private void deleteUserTourOffers(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_TOUR_OFFERS)
                .whereEqualTo("guideId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentReference> toDelete = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        toDelete.add(doc.getReference());
                    }
                    deleteDocuments(toDelete, "ofertas de tours", callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error buscando ofertas de tours", e);
                    callback.onSuccess(true);
                });
    }

    /**
     * Elimina múltiples documentos usando batch writes
     */
    private void deleteDocuments(List<DocumentReference> documents, String typeName, FirestoreCallback callback) {
        if (documents.isEmpty()) {
            Log.d(TAG, "No hay " + typeName + " para eliminar");
            callback.onSuccess(true);
            return;
        }

        Log.d(TAG, "Eliminando " + documents.size() + " " + typeName);
        
        // Firestore permite máximo 500 operaciones por batch
        final int BATCH_SIZE = 500;
        int totalBatches = (documents.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        final int[] completedBatches = {0};
        final boolean[] hasError = {false};

        for (int i = 0; i < totalBatches; i++) {
            int start = i * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, documents.size());
            List<DocumentReference> batch = documents.subList(start, end);

            WriteBatch writeBatch = db.batch();
            for (DocumentReference docRef : batch) {
                writeBatch.delete(docRef);
            }

            writeBatch.commit()
                    .addOnSuccessListener(aVoid -> {
                        synchronized (completedBatches) {
                            completedBatches[0]++;
                            if (completedBatches[0] >= totalBatches) {
                                if (!hasError[0]) {
                                    Log.d(TAG, "Todas las " + typeName + " eliminadas correctamente");
                                }
                                callback.onSuccess(true);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        synchronized (completedBatches) {
                            hasError[0] = true;
                            completedBatches[0]++;
                            Log.e(TAG, "Error eliminando batch de " + typeName, e);
                            if (completedBatches[0] >= totalBatches) {
                                callback.onSuccess(true); // Continuar aunque haya errores
                            }
                        }
                    });
        }
    }

    /**
     * Elimina los datos principales del usuario (users, guides, user_roles)
     */
    private void deleteUserMainData(String userId, String userType, FirestoreCallback callback) {
        // Eliminar de guides si es GUIDE
        if ("GUIDE".equals(userType)) {
            db.collection(COLLECTION_GUIDES)
                    .document(userId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Guide eliminado: " + userId);
                        deleteUserRoleAndUser(userId, callback);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "No se pudo eliminar de guides (puede ser normal si no existe): " + e.getMessage());
                        deleteUserRoleAndUser(userId, callback);
                    });
        } else {
            deleteUserRoleAndUser(userId, callback);
        }
    }

    /**
     * Elimina user_roles y luego el usuario
     */
    private void deleteUserRoleAndUser(String userId, FirestoreCallback callback) {
        // Eliminar de user_roles
        db.collection(COLLECTION_USER_ROLES)
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "User role eliminado: " + userId);
                    deleteFinalUser(userId, callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "No se encontró user_roles para eliminar (puede ser normal): " + e.getMessage());
                    deleteFinalUser(userId, callback);
                });
    }

    /**
     * Elimina el documento principal del usuario
     */
    private void deleteFinalUser(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Usuario eliminado completamente: " + userId);
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando usuario principal", e);
                    callback.onFailure(e);
                });
    }

    public void updateUserStatus(String userId, String status, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new Exception("User ID is required"));
            return;
        }
        if (status == null || status.trim().isEmpty()) {
            callback.onFailure(new Exception("Status is required"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        db.collection(COLLECTION_USERS)
                .document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    public Query queryUsersByTypeAndStatus(String userType, String status) {
        Query q = db.collection(COLLECTION_USERS);
        if (userType != null) q = q.whereEqualTo("userType", userType);
        if (status != null) q = q.whereEqualTo("status", status);
        return q;
    }

    private String validateUser(User user) {
        if (user == null) return "User is null";
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) return "User ID is required";
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) return "Email is required";
        if (user.getUserType() == null || user.getUserType().trim().isEmpty()) return "User type is required";

        String t = user.getUserType().trim().toUpperCase();
        if (!t.equals("CLIENT") && !t.equals("GUIDE") && !t.equals("COMPANY_ADMIN") && !t.equals("SUPERADMIN")) {
            return "Invalid userType. Use CLIENT, GUIDE, COMPANY_ADMIN or SUPERADMIN";
        }

        // CLIENT / GUIDE requieren personalData
        if (t.equals("CLIENT") || t.equals("GUIDE")) {
            if (user.getPersonalData() == null) return "personalData is required for CLIENT/GUIDE";
            User.PersonalData pd = user.getPersonalData();
            if (pd.getFirstName() == null || pd.getFirstName().trim().isEmpty()) return "First name is required";
            if (pd.getLastName() == null || pd.getLastName().trim().isEmpty()) return "Last name is required";
            if (pd.getDocumentType() == null || pd.getDocumentType().trim().isEmpty()) return "Document type is required";
            if (pd.getDocumentNumber() == null || pd.getDocumentNumber().trim().isEmpty()) return "Document number is required";
        }

        // COMPANY_ADMIN requiere companyId
        if (t.equals("COMPANY_ADMIN")) {
            if (user.getCompanyId() == null || user.getCompanyId().trim().isEmpty()) return "companyId is required for COMPANY_ADMIN";
        }

        // status default si viene vacío
        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            // no lo forzamos aquí, pero sería ideal setearlo antes de llamar upsertUser
            Log.w(TAG, "User status is empty; consider setting active/pending_approval");
        }

        return null;
    }

    // ==================== GUIDES ====================

    /**
     * Obtener todos los guías de la colección guides
     */
    public void getAllGuides(FirestoreCallback callback) {
        db.collection(COLLECTION_GUIDES)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Guide> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Guide g = doc.toObject(Guide.class);
                        if (g != null) {
                            if (g.getGuideId() == null || g.getGuideId().isEmpty()) {
                                g.setGuideId(doc.getId());
                            }
                            list.add(g);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void upsertGuide(Guide guide, FirestoreCallback callback) {
        if (guide == null) {
            callback.onFailure(new Exception("Guide is null"));
            return;
        }
        if (guide.getGuideId() == null || guide.getGuideId().trim().isEmpty()) {
            callback.onFailure(new Exception("guideId is required"));
            return;
        }

        db.collection(COLLECTION_GUIDES)
                .document(guide.getGuideId())
                .set(guide, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(guide))
                .addOnFailureListener(callback::onFailure);
    }

    public void approveGuide(String userId, FirestoreCallback callback) {
        // 1) user.status = active
        updateUserStatus(userId, "active", new FirestoreCallback() {
            @Override public void onSuccess(Object result) {
                // 2) guides/{uid}.isApproved = true
                Map<String, Object> updates = new HashMap<>();
                updates.put("approved", true);   // OJO: tu getter es getApproved() / setApproved()
                updates.put("isApproved", true); // para evitar inconsistencia por nombre

                db.collection(COLLECTION_GUIDES)
                        .document(userId)
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener(unused -> callback.onSuccess(true))
                        .addOnFailureListener(callback::onFailure);
            }

            @Override public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    // ==================== COMPANIES ====================

    public void createCompany(Company company, FirestoreCallback callback) {
        if (company == null) {
            callback.onFailure(new Exception("Company is null"));
            return;
        }
        if (company.getBusinessName() == null || company.getBusinessName().trim().isEmpty()) {
            callback.onFailure(new Exception("businessName is required"));
            return;
        }
        if (company.getRuc() == null || company.getRuc().trim().isEmpty()) {
            callback.onFailure(new Exception("ruc is required"));
            return;
        }
        if (company.getAdminUserId() == null || company.getAdminUserId().trim().isEmpty()) {
            callback.onFailure(new Exception("adminUserId is required"));
            return;
        }

        // Si no tienes companyId, crea doc nuevo
        DocumentReference ref = (company.getCompanyId() == null || company.getCompanyId().trim().isEmpty())
                ? db.collection(COLLECTION_COMPANIES).document()
                : db.collection(COLLECTION_COMPANIES).document(company.getCompanyId());

        company.setCompanyId(ref.getId());

        ref.set(company, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(company))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Actualizar campos específicos de una empresa
     */
    public void updateCompany(String companyId, Map<String, Object> updates, FirestoreCallback callback) {
        if (companyId == null || companyId.trim().isEmpty()) {
            callback.onFailure(new Exception("companyId is required"));
            return;
        }
        if (updates == null || updates.isEmpty()) {
            callback.onFailure(new Exception("updates is required"));
            return;
        }

        db.collection(COLLECTION_COMPANIES)
                .document(companyId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Company updated: " + companyId);
                    callback.onSuccess(true);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getCompanyById(String companyId, FirestoreCallback callback) {
        if (companyId == null || companyId.trim().isEmpty()) {
            callback.onFailure(new Exception("companyId is required"));
            return;
        }

        db.collection(COLLECTION_COMPANIES)
                .document(companyId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure(new Exception("Company not found"));
                        return;
                    }
                    Company c = doc.toObject(Company.class);
                    if (c != null && c.getCompanyId() == null) c.setCompanyId(doc.getId());
                    callback.onSuccess(c);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== SERVICES ====================

    /**
     * Crear un nuevo servicio
     */
    public void createService(com.example.droidtour.models.Service service, FirestoreCallback callback) {
        if (service == null) {
            callback.onFailure(new Exception("Service is null"));
            return;
        }
        if (service.getCompanyId() == null || service.getCompanyId().trim().isEmpty()) {
            callback.onFailure(new Exception("companyId is required"));
            return;
        }
        if (service.getName() == null || service.getName().trim().isEmpty()) {
            callback.onFailure(new Exception("Service name is required"));
            return;
        }

        // Crear o asegurar serviceId
        DocumentReference serviceRef = (service.getServiceId() == null || service.getServiceId().trim().isEmpty())
                ? db.collection(COLLECTION_SERVICES).document()
                : db.collection(COLLECTION_SERVICES).document(service.getServiceId());
        service.setServiceId(serviceRef.getId());

        // Establecer fecha de creación
        service.setCreatedAt(new java.util.Date());
        service.setUpdatedAt(new java.util.Date());

        // Establecer status por defecto si no existe
        if (service.getStatus() == null || service.getStatus().trim().isEmpty()) {
            service.setStatus("active");
        }

        serviceRef.set(service)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Service created: " + service.getServiceId());
                    callback.onSuccess(service);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Actualizar un servicio existente
     */
    public void updateService(com.example.droidtour.models.Service service, FirestoreCallback callback) {
        if (service == null) {
            callback.onFailure(new Exception("Service is null"));
            return;
        }
        if (service.getServiceId() == null || service.getServiceId().trim().isEmpty()) {
            callback.onFailure(new Exception("serviceId is required"));
            return;
        }

        // Actualizar fecha de modificación
        service.setUpdatedAt(new java.util.Date());

        db.collection(COLLECTION_SERVICES)
                .document(service.getServiceId())
                .set(service, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Service updated: " + service.getServiceId());
                    callback.onSuccess(service);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener un servicio por su ID
     */
    public void getServiceById(String serviceId, FirestoreCallback callback) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            callback.onFailure(new Exception("serviceId is required"));
            return;
        }

        db.collection(COLLECTION_SERVICES)
                .document(serviceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure(new Exception("Service not found"));
                        return;
                    }
                    com.example.droidtour.models.Service service = doc.toObject(com.example.droidtour.models.Service.class);
                    if (service != null && service.getServiceId() == null) {
                        service.setServiceId(doc.getId());
                    }
                    callback.onSuccess(service);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener todos los servicios de una empresa
     */
    public void getServicesByCompany(String companyId, FirestoreCallback callback) {
        if (companyId == null || companyId.trim().isEmpty()) {
            callback.onFailure(new Exception("companyId is required"));
            return;
        }

        db.collection(COLLECTION_SERVICES)
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<com.example.droidtour.models.Service> services = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        com.example.droidtour.models.Service service = doc.toObject(com.example.droidtour.models.Service.class);
                        if (service != null) {
                            if (service.getServiceId() == null || service.getServiceId().isEmpty()) {
                                service.setServiceId(doc.getId());
                            }
                            services.add(service);
                        }
                    }
                    callback.onSuccess(services);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Eliminar un servicio
     */
    public void deleteService(String serviceId, FirestoreCallback callback) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            callback.onFailure(new Exception("serviceId is required"));
            return;
        }

        db.collection(COLLECTION_SERVICES)
                .document(serviceId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Service deleted: " + serviceId);
                    callback.onSuccess(true);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== NUEVOS MÉTODOS ====================

    /**
     * Obtener ofertas dirigidas a un guía
     */
    public void getOffersByGuide(String guideId, FirestoreCallback callback) {
        if (guideId == null || guideId.trim().isEmpty()) {
            callback.onFailure(new Exception("guideId is required"));
            return;
        }

        db.collection(COLLECTION_TOUR_OFFERS)
                .whereEqualTo("guideId", guideId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TourOffer> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TourOffer t = doc.toObject(TourOffer.class);
                        if (t != null && (t.getOfferId() == null || t.getOfferId().isEmpty())) t.setOfferId(doc.getId());
                        list.add(t);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener ofertas por companyId (para admin)
     */
    public void getOffersByCompany(String companyId, FirestoreCallback callback) {
        if (companyId == null || companyId.trim().isEmpty()) {
            callback.onFailure(new Exception("companyId is required"));
            return;
        }

        db.collection(COLLECTION_TOUR_OFFERS)
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TourOffer> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TourOffer t = doc.toObject(TourOffer.class);
                        if (t != null && (t.getOfferId() == null || t.getOfferId().isEmpty())) {
                            t.setOfferId(doc.getId());
                        }
                        list.add(t);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener reservas asociadas a un guía
     */
    public void getReservationsByGuide(String guideId, FirestoreCallback callback) {
        if (guideId == null || guideId.trim().isEmpty()) {
            callback.onFailure(new Exception("guideId is required"));
            return;
        }

        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("guideId", guideId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Reservation> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Reservation r = doc.toObject(Reservation.class);
                        if (r != null && (r.getReservationId() == null || r.getReservationId().isEmpty())) r.setReservationId(doc.getId());
                        list.add(r);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener notificaciones no leídas de un usuario
     */
    public void getUnreadNotifications(String userId, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new Exception("userId is required"));
            return;
        }

        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null && (n.getNotificationId() == null || n.getNotificationId().isEmpty())) n.setNotificationId(doc.getId());
                        list.add(n);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Crear una nueva oferta de tour (propuesta a guía)
     */
    public void createTourOffer(TourOffer offer, FirestoreCallback callback) {
        if (offer == null) {
            callback.onFailure(new Exception("TourOffer is null"));
            return;
        }
        if (offer.getGuideId() == null || offer.getGuideId().trim().isEmpty()) {
            callback.onFailure(new Exception("guideId is required"));
            return;
        }
        if (offer.getTourId() == null || offer.getTourId().trim().isEmpty()) {
            callback.onFailure(new Exception("tourId is required"));
            return;
        }
        if (offer.getAgencyId() == null || offer.getAgencyId().trim().isEmpty()) {
            callback.onFailure(new Exception("agencyId is required"));
            return;
        }

        // Crear documento con ID automático
        DocumentReference docRef = db.collection(COLLECTION_TOUR_OFFERS).document();
        offer.setId(docRef.getId());

        // Establecer campos por defecto
        if (offer.getStatus() == null || offer.getStatus().trim().isEmpty()) {
            offer.setStatus("PENDIENTE");
        }

        docRef.set(offer)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "TourOffer created with ID: " + docRef.getId());
                    callback.onSuccess(offer);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating TourOffer", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar estado de una oferta (ACEPTADA / RECHAZADA)
     */
    public void updateOfferStatus(String offerId, String status, FirestoreCallback callback) {
        if (offerId == null || offerId.trim().isEmpty()) {
            callback.onFailure(new Exception("offerId is required"));
            return;
        }
        if (status == null || status.trim().isEmpty()) {
            callback.onFailure(new Exception("status is required"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection(COLLECTION_TOUR_OFFERS)
                .document(offerId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener un tour por id
     */
    public void getTour(String tourId, FirestoreCallback callback) {
        if (tourId == null || tourId.trim().isEmpty()) {
            callback.onFailure(new Exception("tourId is required"));
            return;
        }

        db.collection(COLLECTION_TOURS)
                .document(tourId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure(new Exception("Tour not found"));
                        return;
                    }
                    Tour t = doc.toObject(Tour.class);
                    if (t != null && t.getTourId() == null) t.setTourId(doc.getId());
                    callback.onSuccess(t);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener un tour por id con callback específico para Tour
     */
    public void getTourById(String tourId, TourCallback callback) {
        if (tourId == null || tourId.trim().isEmpty()) {
            callback.onFailure("tourId is required");
            return;
        }

        db.collection(COLLECTION_TOURS)
                .document(tourId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onSuccess(null);
                        return;
                    }
                    Tour t = doc.toObject(Tour.class);
                    if (t != null && t.getTourId() == null) t.setTourId(doc.getId());
                    callback.onSuccess(t);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Verificar si un usuario ya tiene una reserva confirmada para un tour
     */
    public void hasConfirmedReservation(String userId, String tourId, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new Exception("userId is required"));
            return;
        }
        if (tourId == null || tourId.trim().isEmpty()) {
            callback.onFailure(new Exception("tourId is required"));
            return;
        }

        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("tourId", tourId)
                .whereEqualTo("status", "CONFIRMADA")
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    boolean exists = !qs.isEmpty();
                    callback.onSuccess(exists);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== PAGOS / MÉTODOS DE PAGO ====================

    public void getPaymentMethodsByUser(String userId, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new Exception("userId is required"));
            return;
        }
        db.collection(COLLECTION_PAYMENT_METHODS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(qs -> {
                    List<PaymentMethod> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        PaymentMethod pm = doc.toObject(PaymentMethod.class);
                        if (pm != null && (pm.getPaymentMethodId() == null || pm.getPaymentMethodId().isEmpty())) pm.setPaymentMethodId(doc.getId());
                        list.add(pm);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Agregar un nuevo método de pago
     */
    public void addPaymentMethod(PaymentMethod paymentMethod, FirestoreCallback callback) {
        if (paymentMethod == null) {
            callback.onFailure(new Exception("PaymentMethod is null"));
            return;
        }
        if (paymentMethod.getUserId() == null || paymentMethod.getUserId().trim().isEmpty()) {
            callback.onFailure(new Exception("userId is required"));
            return;
        }

        // Crear documento con ID automático
        DocumentReference docRef = db.collection(COLLECTION_PAYMENT_METHODS).document();
        paymentMethod.setPaymentMethodId(docRef.getId());

        docRef.set(paymentMethod.toMap())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "PaymentMethod added with ID: " + docRef.getId());
                    callback.onSuccess(paymentMethod);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding PaymentMethod", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar un método de pago existente (con objeto PaymentMethod)
     */
    public void updatePaymentMethod(PaymentMethod paymentMethod, FirestoreCallback callback) {
        if (paymentMethod == null || paymentMethod.getPaymentMethodId() == null) {
            callback.onFailure(new Exception("PaymentMethod or paymentMethodId is null"));
            return;
        }

        db.collection(COLLECTION_PAYMENT_METHODS)
                .document(paymentMethod.getPaymentMethodId())
                .set(paymentMethod.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "PaymentMethod updated: " + paymentMethod.getPaymentMethodId());
                    callback.onSuccess(paymentMethod);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating PaymentMethod", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar un método de pago existente (con Map de campos)
     */
    public void updatePaymentMethod(String paymentMethodId, Map<String, Object> updates, FirestoreCallback callback) {
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            callback.onFailure(new Exception("paymentMethodId is required"));
            return;
        }
        if (updates == null || updates.isEmpty()) {
            callback.onFailure(new Exception("updates is required"));
            return;
        }

        db.collection(COLLECTION_PAYMENT_METHODS)
                .document(paymentMethodId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "PaymentMethod updated: " + paymentMethodId);
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating PaymentMethod", e);
                    callback.onFailure(e);
                });
    }

    public void setDefaultPaymentMethod(String userId, String paymentMethodId, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) { callback.onFailure(new Exception("userId is required")); return; }
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) { callback.onFailure(new Exception("paymentMethodId is required")); return; }

        // 1) actualizar campo defaultPaymentMethodId en el documento del usuario
        Map<String, Object> updates = new HashMap<>();
        updates.put("defaultPaymentMethodId", paymentMethodId);

        db.collection(COLLECTION_USERS).document(userId).set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // 2) marcar todos los métodos del usuario como isDefault=false y luego el elegido como true
                    db.collection(COLLECTION_PAYMENT_METHODS).whereEqualTo("userId", userId).get()
                            .addOnSuccessListener(qs -> {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                                    Map<String, Object> upd = new HashMap<>();
                                    boolean isDefault = paymentMethodId.equals(doc.getId());
                                    upd.put("isDefault", isDefault);
                                    db.collection(COLLECTION_PAYMENT_METHODS).document(doc.getId()).set(upd, SetOptions.merge());
                                }
                                callback.onSuccess(true);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void deletePaymentMethod(String paymentMethodId, FirestoreCallback callback) {
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) { callback.onFailure(new Exception("paymentMethodId is required")); return; }
        db.collection(COLLECTION_PAYMENT_METHODS).document(paymentMethodId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== COMPANIES & TOURS ====================

    public void getCompanies(FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES).get()
                .addOnSuccessListener(qs -> {
                    List<Company> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Company c = doc.toObject(Company.class);
                        if (c != null && (c.getCompanyId() == null || c.getCompanyId().isEmpty())) c.setCompanyId(doc.getId());
                        list.add(c);
                    }
                    callback.onSuccess(list);
                }).addOnFailureListener(callback::onFailure);
    }

    public void getCompany(String companyId, FirestoreCallback callback) {
        getCompanyById(companyId, callback);
    }

    /**
     * Obtener tours públicos de una empresa (para clientes)
     */
    public void getToursByCompany(String companyId, FirestoreCallback callback) {
        if (companyId == null || companyId.trim().isEmpty()) { callback.onFailure(new Exception("companyId is required")); return; }
        db.collection(COLLECTION_TOURS)
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("isPublic", true)  // Solo tours públicos (con guía asignado)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Tour> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Tour t = doc.toObject(Tour.class);
                        if (t != null && (t.getTourId() == null || t.getTourId().isEmpty())) t.setTourId(doc.getId());
                        list.add(t);
                    }
                    callback.onSuccess(list);
                }).addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener TODOS los tours de una empresa (para admin/gestión interna)
     */
    public void getAllToursByCompany(String companyId, FirestoreCallback callback) {
        if (companyId == null || companyId.trim().isEmpty()) { 
            callback.onFailure(new Exception("companyId is required")); 
            return; 
        }
        db.collection(COLLECTION_TOURS)
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Tour> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Tour t = doc.toObject(Tour.class);
                        if (t != null && (t.getTourId() == null || t.getTourId().isEmpty())) 
                            t.setTourId(doc.getId());
                        list.add(t);
                    }
                    callback.onSuccess(list);
                }).addOnFailureListener(callback::onFailure);
    }

    /**
     * Crear un nuevo tour
     */
    public void createTour(Tour tour, FirestoreCallback callback) {
        if (tour == null) {
            callback.onFailure(new Exception("Tour is null"));
            return;
        }

        DocumentReference docRef = db.collection(COLLECTION_TOURS).document();
        tour.setTourId(docRef.getId());

        docRef.set(tour.toMap())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Tour created with ID: " + docRef.getId());
                    callback.onSuccess(tour);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating tour", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar un tour existente (con objeto Tour completo)
     */
    public void updateTour(Tour tour, FirestoreCallback callback) {
        if (tour == null || tour.getTourId() == null) {
            callback.onFailure(new Exception("Tour or tourId is null"));
            return;
        }

        db.collection(COLLECTION_TOURS)
                .document(tour.getTourId())
                .set(tour.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Tour updated: " + tour.getTourId());
                    callback.onSuccess(tour);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating tour", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar campos específicos de un tour (con Map)
     */
    public void updateTour(String tourId, Map<String, Object> updates, FirestoreCallback callback) {
        if (tourId == null || tourId.trim().isEmpty()) {
            callback.onFailure(new Exception("tourId is required"));
            return;
        }
        if (updates == null || updates.isEmpty()) {
            callback.onFailure(new Exception("updates map is required"));
            return;
        }

        db.collection(COLLECTION_TOURS)
                .document(tourId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Tour fields updated: " + tourId);
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating tour fields", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Eliminar un tour
     */
    public void deleteTour(String tourId, FirestoreCallback callback) {
        if (tourId == null || tourId.trim().isEmpty()) {
            callback.onFailure(new Exception("tourId is required"));
            return;
        }

        db.collection(COLLECTION_TOURS)
                .document(tourId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Tour deleted: " + tourId);
                    callback.onSuccess(true);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener todos los tours (para admin)
     */
    public void getAllTours(FirestoreCallback callback) {
        db.collection(COLLECTION_TOURS).get()
                .addOnSuccessListener(qs -> {
                    List<Tour> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Tour t = doc.toObject(Tour.class);
                        if (t != null && (t.getTourId() == null || t.getTourId().isEmpty())) t.setTourId(doc.getId());
                        list.add(t);
                    }
                    callback.onSuccess(list);
                }).addOnFailureListener(callback::onFailure);
    }

    // ==================== CREAR/ACTUALIZAR USUARIOS & ROLES ====================

    public void createUser(User user, FirestoreCallback callback) {
        if (user == null) { callback.onFailure(new Exception("User is null")); return; }
        // Similar a upsertUser pero permite generar id
        DocumentReference ref = (user.getUserId() == null || user.getUserId().trim().isEmpty())
                ? db.collection(COLLECTION_USERS).document()
                : db.collection(COLLECTION_USERS).document(user.getUserId());
        user.setUserId(ref.getId());
        
        // Log para debugging
        if (user.getPersonalData() != null && user.getPersonalData().getProfileImageUrl() != null) {
            Log.d(TAG, "Guardando usuario con imagen URL: " + user.getPersonalData().getProfileImageUrl());
        }
        
        ref.set(user, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Usuario creado exitosamente. userId: " + user.getUserId());
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al crear usuario: " + e.getMessage(), e);
                    callback.onFailure(e);
                });
    }

    public void createOrUpdateUser(User user, FirestoreCallback callback) {
        createUser(user, callback);
    }

    public void saveUserRole(String userId, String userType, String roleStatus, FirestoreCallback callback) {
        saveUserRole(userId, userType, roleStatus, null, callback);
    }

    public void saveUserRole(String userId, String userType, String roleStatus, Map<String, Object> extraFields, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) { callback.onFailure(new Exception("userId is required")); return; }
        Map<String, Object> data = new HashMap<>();
        data.put("userType", userType);
        data.put("status", roleStatus);
        if (extraFields != null) data.putAll(extraFields);
        db.collection(COLLECTION_USER_ROLES).document(userId).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserRoles(String userId, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) { callback.onFailure(new Exception("userId is required")); return; }
        db.collection(COLLECTION_USER_ROLES).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { callback.onFailure(new Exception("User roles not found")); return; }
                    callback.onSuccess(doc.getData());
                }).addOnFailureListener(callback::onFailure);
    }

    // ==================== NOTIFICACIONES ====================

    public void getNotificationsByUser(String userId, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) { callback.onFailure(new Exception("userId is required")); return; }
        db.collection(COLLECTION_NOTIFICATIONS).whereEqualTo("userId", userId).get()
                .addOnSuccessListener(qs -> {
                    List<Notification> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null && (n.getNotificationId() == null || n.getNotificationId().isEmpty())) n.setNotificationId(doc.getId());
                        list.add(n);
                    }
                    callback.onSuccess(list);
                }).addOnFailureListener(callback::onFailure);
    }

    public void updateNotification(String notificationId, Map<String, Object> updates, FirestoreCallback callback) {
        if (notificationId == null || notificationId.trim().isEmpty()) { callback.onFailure(new Exception("notificationId is required")); return; }
        db.collection(COLLECTION_NOTIFICATIONS).document(notificationId).set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteNotification(String notificationId, FirestoreCallback callback) {
        if (notificationId == null || notificationId.trim().isEmpty()) { callback.onFailure(new Exception("notificationId is required")); return; }
        db.collection(COLLECTION_NOTIFICATIONS).document(notificationId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== SESSIONS, RESERVAS, REVIEWS ====================

    public void createUserSession(UserSession session, FirestoreCallback callback) {
        if (session == null) { callback.onFailure(new Exception("session is null")); return; }
        DocumentReference ref = (session.getSessionId() == null || session.getSessionId().isEmpty())
                ? db.collection(COLLECTION_USER_SESSIONS).document()
                : db.collection(COLLECTION_USER_SESSIONS).document(session.getSessionId());
        session.setSessionId(ref.getId());
        ref.set(session, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(session))
                .addOnFailureListener(callback::onFailure);
    }

    public void getReservationsByUser(String userId, FirestoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) { callback.onFailure(new Exception("userId is required")); return; }
        db.collection(COLLECTION_RESERVATIONS).whereEqualTo("userId", userId).get()
                .addOnSuccessListener(qs -> {
                    List<Reservation> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Reservation r = doc.toObject(Reservation.class);
                        if (r != null && (r.getReservationId() == null || r.getReservationId().isEmpty())) r.setReservationId(doc.getId());
                        list.add(r);
                    }
                    callback.onSuccess(list);
                }).addOnFailureListener(callback::onFailure);
    }

    public void createReservation(Reservation reservation, FirestoreCallback callback) {
        if (reservation == null) { callback.onFailure(new Exception("reservation is null")); return; }
        DocumentReference ref = (reservation.getReservationId() == null || reservation.getReservationId().isEmpty())
                ? db.collection(COLLECTION_RESERVATIONS).document()
                : db.collection(COLLECTION_RESERVATIONS).document(reservation.getReservationId());
        reservation.setReservationId(ref.getId());
        ref.set(reservation, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(reservation))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateReservation(String reservationId, Map<String, Object> updates, FirestoreCallback callback) {
        if (reservationId == null || reservationId.trim().isEmpty()) { callback.onFailure(new Exception("reservationId is required")); return; }
        db.collection(COLLECTION_RESERVATIONS).document(reservationId).set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener reservaciones por companyId
     */
    public void getReservationsByCompany(String companyId, FirestoreCallback callback) {
        if (companyId == null || companyId.trim().isEmpty()) {
            callback.onFailure(new Exception("companyId is required"));
            return;
        }
        
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Reservation> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Reservation r = doc.toObject(Reservation.class);
                        if (r != null && (r.getReservationId() == null || r.getReservationId().isEmpty())) {
                            r.setReservationId(doc.getId());
                        }
                        list.add(r);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Actualizar estado de una reservación
     */
    public void updateReservationStatus(String reservationId, String newStatus, FirestoreCallback callback) {
        if (reservationId == null || reservationId.trim().isEmpty()) {
            callback.onFailure(new Exception("reservationId is required"));
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        
        db.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Reservation status updated: " + reservationId + " -> " + newStatus);
                    callback.onSuccess(true);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== REVIEWS ====================

    public void createReview(String tourId, String userId, int rating, String comment, FirestoreCallback callback) {
        Review review = new Review();
        review.setTourId(tourId);
        review.setUserId(userId);
        // convertir a Float
        review.setRating((float) rating);
        review.setComment(comment);
        // Guardar con id generado
        DocumentReference ref = db.collection(COLLECTION_REVIEWS).document();
        ref.set(review, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== ALIAS / COMPATIBILIDAD ====================

    public void getUser(String userId, FirestoreCallback callback) {
        getUserById(userId, callback);
    }

    public void getTour(String tourId) {
        // placeholder - existing getTour(String, FirestoreCallback) should be used
    }

    // ==================== MENSAJES ====================

    /**
     * Obtener mensajes por companyId (para admin)
     */
    public void getMessagesByCompany(String companyId, FirestoreCallback callback) {
        if (companyId == null || companyId.trim().isEmpty()) {
            callback.onFailure(new Exception("companyId is required"));
            return;
        }
        
        db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Message> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Message m = doc.toObject(Message.class);
                        if (m != null) {
                            if (m.getMessageId() == null || m.getMessageId().isEmpty()) {
                                m.setMessageId(doc.getId());
                            }
                            list.add(m);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Obtener mensajes entre un cliente y una empresa
     */
    public void getMessagesBetweenClientAndCompany(String clientId, String companyId, FirestoreCallback callback) {
        if (clientId == null || companyId == null) {
            callback.onFailure(new Exception("clientId and companyId are required"));
            return;
        }
        
        db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("senderId", clientId)
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Message> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        Message m = doc.toObject(Message.class);
                        if (m != null) list.add(m);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Enviar mensaje
     */
    public void sendMessage(Message message, FirestoreCallback callback) {
        if (message == null) {
            callback.onFailure(new Exception("Message is null"));
            return;
        }
        
        DocumentReference docRef = db.collection(COLLECTION_MESSAGES).document();
        message.setMessageId(docRef.getId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("messageId", message.getMessageId());
        data.put("senderId", message.getSenderId());
        data.put("senderName", message.getSenderName());
        data.put("receiverId", message.getReceiverId());
        data.put("companyId", message.getCompanyId());
        data.put("content", message.getContent());
        data.put("isRead", false);
        data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        docRef.set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(message))
                .addOnFailureListener(callback::onFailure);
    }


    public void getAllCompanies(FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES)
                .whereEqualTo("status", "active") // Solo empresas activas
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Company> companies = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Company company = document.toObject(Company.class);
                            company.setCompanyId(document.getId());
                            companies.add(company);
                        }
                        callback.onSuccess(companies);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // También podrías necesitar una versión sin filtro:
    public void getAllCompaniesNoFilter(FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Company> companies = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Company company = document.toObject(Company.class);
                            company.setCompanyId(document.getId());
                            companies.add(company);
                        }
                        callback.onSuccess(companies);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ==================== CALLBACK ====================

    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onFailure(Exception e);
    }

    public interface TourCallback {
        void onSuccess(Tour tour);
        void onFailure(String error);
    }
}
