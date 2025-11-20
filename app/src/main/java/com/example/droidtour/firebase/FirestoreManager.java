package com.example.droidtour.firebase;

import android.util.Log;

import com.example.droidtour.models.*;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manager para Firebase Firestore
 * Proporciona métodos CRUD para todas las colecciones de la base de datos
 */
public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;
    
    private final FirebaseFirestore db;
    
    // Nombres de colecciones
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_COMPANIES = "companies";
    private static final String COLLECTION_TOURS = "tours";
    private static final String COLLECTION_RESERVATIONS = "reservations";
    private static final String COLLECTION_REVIEWS = "reviews";
    private static final String COLLECTION_PAYMENT_METHODS = "payment_methods";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_USER_PREFERENCES = "user_preferences";
    private static final String COLLECTION_MESSAGES = "messages";
    private static final String COLLECTION_USER_SESSIONS = "user_sessions";

    private FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    // ==================== USUARIOS ====================

    /**
     * Crear un nuevo usuario en Firestore
     */
    public void createUser(User user, FirestoreCallback callback) {
        String userId = user.getUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new Exception("User ID is required"));
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(userId)
                .set(user.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User created successfully");
                    callback.onSuccess(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener usuario por ID (alias)
     */
    public void getUser(String userId, FirestoreCallback callback) {
        getUserById(userId, callback);
    }
    
    /**
     * Obtener usuario por ID
     */
    public void getUserById(String userId, FirestoreCallback callback) {
        Log.d(TAG, "🔍 getUserById llamado con userId: '" + userId + "'");
        Log.d(TAG, "🔍 Buscando en: " + COLLECTION_USERS + "/" + userId);
        
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ userId es null o vacío");
            callback.onFailure(new Exception("userId is null or empty"));
            return;
        }
        
        db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "🔍 Respuesta de Firestore - exists: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "✅ Documento encontrado!");
                        User user = documentSnapshot.toObject(User.class);
                        Log.d(TAG, "✅ Usuario deserializado: " + (user != null ? user.getEmail() : "null"));
                        callback.onSuccess(user);
                    } else {
                        Log.e(TAG, "❌ Documento NO existe en Firestore");
                        Log.e(TAG, "❌ Ruta buscada: users/" + userId);
                        callback.onFailure(new Exception("User not found in Firestore at path: users/" + userId));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error de red/permisos al obtener usuario", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar usuario
     */
    public void updateUser(String userId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User updated successfully");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener usuarios por tipo (CLIENT, GUIDE, ADMIN, SUPERADMIN)
     */
    public void getUsersByType(String userType, FirestoreCallback callback) {
        db.collection(COLLECTION_USERS)
                .whereEqualTo("userType", userType)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        User user = document.toObject(User.class);
                        users.add(user);
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users by type", e);
                    callback.onFailure(e);
                });
    }
    
    /**
     * Actualizar URL de foto de perfil del usuario
     */
    public void updateUserPhotoUrl(String userId, String photoUrl, FirestoreCallback callback) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .update("photoUrl", photoUrl)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User photo URL updated successfully");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user photo URL", e);
                    callback.onFailure(e);
                });
    }

    // ==================== EMPRESAS ====================

    /**
     * Crear una nueva empresa
     */
    public void createCompany(Company company, FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES)
                .add(company.toMap())
                .addOnSuccessListener(documentReference -> {
                    String companyId = documentReference.getId();
                    Log.d(TAG, "Company created with ID: " + companyId);
                    callback.onSuccess(companyId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating company", e);
                    callback.onFailure(e);
                });
    }
    
    /**
     * Crear una nueva empresa con un ID específico
     */
    public void createCompanyWithId(String companyId, Company company, FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES)
                .document(companyId)
                .set(company.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Company created with custom ID: " + companyId);
                    callback.onSuccess(companyId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating company with ID", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener empresa por ID (alias)
     */
    public void getCompany(String companyId, FirestoreCallback callback) {
        getCompanyById(companyId, callback);
    }
    
    /**
     * Obtener empresa por ID
     */
    public void getCompanyById(String companyId, FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES)
                .document(companyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Company company = documentSnapshot.toObject(Company.class);
                        callback.onSuccess(company);
                    } else {
                        callback.onFailure(new Exception("Company not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting company", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener todas las empresas activas
     */
    public void getAllActiveCompanies(FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Company> companies = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Company company = document.toObject(Company.class);
                        companies.add(company);
                    }
                    callback.onSuccess(companies);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting companies", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar empresa
     */
    public void updateCompany(String companyId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES)
                .document(companyId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Company updated successfully");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating company", e);
                    callback.onFailure(e);
                });
    }

    // ==================== TOURS ====================

    /**
     * Crear un nuevo tour
     */
    public void createTour(Tour tour, FirestoreCallback callback) {
        db.collection(COLLECTION_TOURS)
                .add(tour.toMap())
                .addOnSuccessListener(documentReference -> {
                    String tourId = documentReference.getId();
                    Log.d(TAG, "Tour created with ID: " + tourId);
                    callback.onSuccess(tourId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating tour", e);
                    callback.onFailure(e);
                });
    }
    
    /**
     * Crear un nuevo tour con un ID específico
     */
    public void createTourWithId(String tourId, Tour tour, FirestoreCallback callback) {
        db.collection(COLLECTION_TOURS)
                .document(tourId)
                .set(tour.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tour created with custom ID: " + tourId);
                    callback.onSuccess(tourId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating tour with ID", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener tour por ID (alias)
     */
    public void getTour(String tourId, FirestoreCallback callback) {
        getTourById(tourId, callback);
    }
    
    /**
     * Obtener todos los tours (sin filtro)
     */
    public void getTours(FirestoreCallback callback) {
        db.collection(COLLECTION_TOURS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Tour> tours = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Tour tour = document.toObject(Tour.class);
                        tours.add(tour);
                    }
                    callback.onSuccess(tours);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting tours", e);
                    callback.onFailure(e);
                });
    }
    
    /**
     * Obtener empresas (sin filtro)
     */
    public void getCompanies(FirestoreCallback callback) {
        db.collection(COLLECTION_COMPANIES)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Company> companies = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Company company = document.toObject(Company.class);
                        companies.add(company);
                    }
                    callback.onSuccess(companies);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting companies", e);
                    callback.onFailure(e);
                });
    }
    
    /**
     * Obtener tour por ID
     */
    public void getTourById(String tourId, FirestoreCallback callback) {
        db.collection(COLLECTION_TOURS)
                .document(tourId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Tour tour = documentSnapshot.toObject(Tour.class);
                        callback.onSuccess(tour);
                    } else {
                        callback.onFailure(new Exception("Tour not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting tour", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener tours por empresa
     */
    public void getToursByCompany(String companyId, FirestoreCallback callback) {
        db.collection(COLLECTION_TOURS)
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Tour> tours = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Tour tour = document.toObject(Tour.class);
                        tours.add(tour);
                    }
                    callback.onSuccess(tours);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting tours by company", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener tours destacados
     */
    public void getFeaturedTours(int limit, FirestoreCallback callback) {
        db.collection(COLLECTION_TOURS)
                .whereEqualTo("isFeatured", true)
                .whereEqualTo("isActive", true)
                .orderBy("averageRating", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Tour> tours = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Tour tour = document.toObject(Tour.class);
                        tours.add(tour);
                    }
                    callback.onSuccess(tours);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting featured tours", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar tour
     */
    public void updateTour(String tourId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection(COLLECTION_TOURS)
                .document(tourId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tour updated successfully");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating tour", e);
                    callback.onFailure(e);
                });
    }

    // ==================== RESERVAS ====================

    /**
     * Crear una nueva reserva
     */
    public void createReservation(Reservation reservation, FirestoreCallback callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .add(reservation.toMap())
                .addOnSuccessListener(documentReference -> {
                    String reservationId = documentReference.getId();
                    Log.d(TAG, "Reservation created with ID: " + reservationId);
                    callback.onSuccess(reservationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating reservation", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener reserva por ID
     */
    public void getReservationById(String reservationId, FirestoreCallback callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Reservation reservation = documentSnapshot.toObject(Reservation.class);
                        callback.onSuccess(reservation);
                    } else {
                        callback.onFailure(new Exception("Reservation not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting reservation", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener reservas por usuario
     * Nota: No usamos orderBy en Firestore para evitar requerir índice compuesto
     */
    public void getReservationsByUser(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Reservation> reservations = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Reservation reservation = document.toObject(Reservation.class);
                        reservations.add(reservation);
                    }
                    
                    // Ordenar en el cliente por fecha (más reciente primero)
                    reservations.sort((a, b) -> {
                        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        }
                        return 0;
                    });
                    
                    callback.onSuccess(reservations);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting reservations by user", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener reservas por empresa
     */
    public void getReservationsByCompany(String companyId, FirestoreCallback callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("companyId", companyId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Reservation> reservations = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Reservation reservation = document.toObject(Reservation.class);
                        reservations.add(reservation);
                    }
                    callback.onSuccess(reservations);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting reservations by company", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar reserva
     */
    public void updateReservation(String reservationId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reservation updated successfully");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating reservation", e);
                    callback.onFailure(e);
                });
    }

    // ==================== RESEÑAS ====================

    /**
     * Crear reseña de un tour (versión simplificada)
     */
    public void createReview(String tourId, String userId, int rating, String comment, FirestoreCallback callback) {
        getUserById(userId, new FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                Review review = new Review();
                review.setTourId(tourId);
                review.setUserId(userId);
                review.setUserName(user.getFirstName() + " " + user.getLastName());
                review.setRating((float) rating);
                review.setReviewText(comment);
                createReview(review, callback);
            }
            
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
    
    /**
     * Obtener reseñas por usuario
     */
    public void getReviewsByUser(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Review review = document.toObject(Review.class);
                        reviews.add(review);
                    }
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting reviews by user", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Crear una nueva reseña
     */
    public void createReview(Review review, FirestoreCallback callback) {
        db.collection(COLLECTION_REVIEWS)
                .add(review.toMap())
                .addOnSuccessListener(documentReference -> {
                    String reviewId = documentReference.getId();
                    Log.d(TAG, "Review created with ID: " + reviewId);
                    
                    // Actualizar estadísticas del tour
                    updateTourRatingStats(review.getTourId());
                    
                    callback.onSuccess(reviewId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating review", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener reseñas por tour
     */
    public void getReviewsByTour(String tourId, FirestoreCallback callback) {
        db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("tourId", tourId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Review review = document.toObject(Review.class);
                        reviews.add(review);
                    }
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting reviews by tour", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar estadísticas de rating de un tour
     */
    private void updateTourRatingStats(String tourId) {
        getReviewsByTour(tourId, new FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Review> reviews = (List<Review>) result;
                
                if (!reviews.isEmpty()) {
                    float totalRating = 0;
                    for (Review review : reviews) {
                        totalRating += review.getRating();
                    }
                    double averageRating = totalRating / reviews.size();
                    
                    Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("averageRating", averageRating);
                    updates.put("totalReviews", reviews.size());
                    
                    updateTour(tourId, updates, new FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            Log.d(TAG, "Tour rating stats updated");
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error updating tour rating stats", e);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error getting reviews for rating update", e);
            }
        });
    }

    // ==================== MÉTODOS DE PAGO (CLIENTE) ====================

    /**
     * Agregar método de pago (tarjeta) para un cliente
     */
    public void addPaymentMethod(PaymentMethod paymentMethod, FirestoreCallback callback) {
        db.collection(COLLECTION_PAYMENT_METHODS)
                .add(paymentMethod.toMap())
                .addOnSuccessListener(documentReference -> {
                    String paymentMethodId = documentReference.getId();
                    Log.d(TAG, "Payment method added with ID: " + paymentMethodId);
                    callback.onSuccess(paymentMethodId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding payment method", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener métodos de pago de un cliente
     */
    public void getPaymentMethodsByUser(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_PAYMENT_METHODS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PaymentMethod> paymentMethods = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        PaymentMethod pm = document.toObject(PaymentMethod.class);
                        paymentMethods.add(pm);
                    }
                    
                    // Ordenar por isDefault (principal primero) y luego por fecha
                    paymentMethods.sort((a, b) -> {
                        // Null-safe: considerar null como false
                        Boolean aIsDefault = a.getIsDefault() != null ? a.getIsDefault() : false;
                        Boolean bIsDefault = b.getIsDefault() != null ? b.getIsDefault() : false;
                        
                        // Primero, la tarjeta principal
                        if (aIsDefault && !bIsDefault) return -1;
                        if (!aIsDefault && bIsDefault) return 1;
                        
                        // Luego, ordenar por fecha de creación (más reciente primero)
                        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        }
                        return 0;
                    });
                    
                    callback.onSuccess(paymentMethods);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting payment methods", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar método de pago
     */
    public void updatePaymentMethod(String paymentMethodId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection(COLLECTION_PAYMENT_METHODS)
                .document(paymentMethodId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment method updated");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating payment method", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Eliminar método de pago
     */
    public void deletePaymentMethod(String paymentMethodId, FirestoreCallback callback) {
        db.collection(COLLECTION_PAYMENT_METHODS)
                .document(paymentMethodId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment method deleted");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting payment method", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Establecer tarjeta como predeterminada
     */
    public void setDefaultPaymentMethod(String userId, String paymentMethodId, FirestoreCallback callback) {
        // Primero, quitar el flag de default de todas las tarjetas del usuario
        db.collection(COLLECTION_PAYMENT_METHODS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().update("isDefault", false);
                    }
                    
                    // Luego, establecer la nueva tarjeta como default
                    Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("isDefault", true);
                    updatePaymentMethod(paymentMethodId, updates, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error setting default payment method", e);
                    callback.onFailure(e);
                });
    }

    // ==================== NOTIFICACIONES (CLIENTE) ====================

    /**
     * Crear notificación
     */
    public void createNotification(Notification notification, FirestoreCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .add(notification.toMap())
                .addOnSuccessListener(documentReference -> {
                    String notificationId = documentReference.getId();
                    Log.d(TAG, "Notification created with ID: " + notificationId);
                    callback.onSuccess(notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating notification", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener notificaciones de un usuario
     */
    public void getNotificationsByUser(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Notification notification = document.toObject(Notification.class);
                        notifications.add(notification);
                    }
                    
                    // Ordenar por fecha (más reciente primero) en el cliente
                    notifications.sort((a, b) -> {
                        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        }
                        return 0;
                    });
                    
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting notifications", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener notificaciones no leídas de un usuario
     * Nota: No usamos orderBy en Firestore para evitar requerir índice compuesto
     */
    public void getUnreadNotifications(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Notification notification = document.toObject(Notification.class);
                        notifications.add(notification);
                    }
                    
                    // Ordenar en el cliente por fecha (más reciente primero)
                    notifications.sort((a, b) -> {
                        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        }
                        return 0;
                    });
                    
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting unread notifications", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener notificaciones importantes de un usuario
     */
    public void getImportantNotifications(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isImportant", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Notification notification = document.toObject(Notification.class);
                        notifications.add(notification);
                    }
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting important notifications", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Marcar notificación como leída
     */
    public void markNotificationAsRead(String notificationId, FirestoreCallback callback) {
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("isRead", true);
        updates.put("readAt", new java.util.Date());
        
        db.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as read");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as read", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    public void markAllNotificationsAsRead(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Map<String, Object> updates = new java.util.HashMap<>();
                        updates.put("isRead", true);
                        updates.put("readAt", new java.util.Date());
                        document.getReference().update(updates);
                    }
                    Log.d(TAG, "All notifications marked as read");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking all notifications as read", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Contar notificaciones no leídas
     */
    public void countUnreadNotifications(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    Log.d(TAG, "Unread notifications count: " + count);
                    callback.onSuccess(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error counting unread notifications", e);
                    callback.onFailure(e);
                });
    }

    // ==================== PREFERENCIAS DE USUARIO (CLIENTE) ====================

    /**
     * Crear preferencias de usuario
     */
    public void createUserPreferences(UserPreferences preferences, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_PREFERENCES)
                .add(preferences.toMap())
                .addOnSuccessListener(documentReference -> {
                    String preferencesId = documentReference.getId();
                    Log.d(TAG, "User preferences created with ID: " + preferencesId);
                    callback.onSuccess(preferencesId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user preferences", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Obtener preferencias de un usuario
     */
    public void getUserPreferences(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_PREFERENCES)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        UserPreferences prefs = querySnapshot.getDocuments().get(0).toObject(UserPreferences.class);
                        callback.onSuccess(prefs);
                    } else {
                        // Si no existe, crear preferencias por defecto
                        UserPreferences defaultPrefs = new UserPreferences(userId);
                        createUserPreferences(defaultPrefs, new FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                callback.onSuccess(defaultPrefs);
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user preferences", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar preferencias de usuario
     */
    public void updateUserPreferences(String preferencesId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_PREFERENCES)
                .document(preferencesId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User preferences updated");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user preferences", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualizar preferencia específica por userId
     */
    public void updateUserPreferenceByUserId(String userId, String preferenceKey, Object value, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_PREFERENCES)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        Map<String, Object> updates = new java.util.HashMap<>();
                        updates.put(preferenceKey, value);
                        updateUserPreferences(docId, updates, callback);
                    } else {
                        callback.onFailure(new Exception("Preferences not found for user"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user preferences", e);
                    callback.onFailure(e);
                });
    }

    // ==================== MENSAJES (CHAT) ====================

    /**
     * Enviar mensaje
     */
    public void sendMessage(String senderId, String senderName, String receiverId, 
                           String receiverName, String senderType, String messageText, 
                           String conversationId, FirestoreCallback callback) {
        String messageId = db.collection(COLLECTION_MESSAGES).document().getId();
        Map<String, Object> message = new java.util.HashMap<>();
        message.put("messageId", messageId);
        message.put("senderId", senderId);
        message.put("senderName", senderName);
        message.put("receiverId", receiverId);
        message.put("receiverName", receiverName);
        message.put("senderType", senderType);
        message.put("messageText", messageText);
        message.put("timestamp", new java.util.Date());
        message.put("isRead", false);
        message.put("conversationId", conversationId);
        
        db.collection(COLLECTION_MESSAGES).document(messageId).set(message)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Message sent with ID: " + messageId);
                callback.onSuccess(messageId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending message", e);
                callback.onFailure(e);
            });
    }

    /**
     * Obtener mensajes de una conversación en tiempo real
     */
    public void getConversationMessages(String conversationId, FirestoreCallback callback) {
        db.collection(COLLECTION_MESSAGES)
            .whereEqualTo("conversationId", conversationId)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening to messages", error);
                    callback.onFailure(error);
                    return;
                }
                List<Map<String, Object>> messages = new java.util.ArrayList<>();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        messages.add(doc.getData());
                    }
                }
                
                // Ordenar por timestamp (más antiguo primero) en el cliente
                messages.sort((a, b) -> {
                    Object timeA = a.get("timestamp");
                    Object timeB = b.get("timestamp");
                    if (timeA instanceof java.util.Date && timeB instanceof java.util.Date) {
                        return ((java.util.Date) timeA).compareTo((java.util.Date) timeB);
                    }
                    return 0;
                });
                
                callback.onSuccess(messages);
            });
    }

    /**
     * Marcar mensajes como leídos
     */
    public void markMessagesAsRead(String conversationId, String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_MESSAGES)
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    batch.update(doc.getReference(), "isRead", true);
                }
                batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Messages marked as read");
                        callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error marking messages as read", e);
                        callback.onFailure(e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting messages to mark as read", e);
                callback.onFailure(e);
            });
    }

    // ==================== SESIONES DE USUARIO ====================

    /**
     * Crear una nueva sesión de usuario al iniciar sesión
     */
    public void createUserSession(UserSession session, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_SESSIONS)
            .add(session.toMap())
            .addOnSuccessListener(documentReference -> {
                String sessionId = documentReference.getId();
                Log.d(TAG, "Session created with ID: " + sessionId);
                callback.onSuccess(sessionId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating session", e);
                callback.onFailure(e);
            });
    }

    /**
     * Obtener sesión activa de un usuario
     */
    public void getActiveUserSession(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_SESSIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isActive", true)
            .orderBy("loginTime", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    UserSession session = querySnapshot.getDocuments().get(0).toObject(UserSession.class);
                    callback.onSuccess(session);
                } else {
                    callback.onFailure(new Exception("No active session found"));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting active session", e);
                callback.onFailure(e);
            });
    }

    /**
     * Obtener todas las sesiones de un usuario (para gestión de dispositivos)
     */
    public void getUserSessions(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_SESSIONS)
            .whereEqualTo("userId", userId)
            .orderBy("loginTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<UserSession> sessions = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    UserSession session = document.toObject(UserSession.class);
                    sessions.add(session);
                }
                callback.onSuccess(sessions);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user sessions", e);
                callback.onFailure(e);
            });
    }

    /**
     * Actualizar actividad de la sesión (mantener sesión viva)
     */
    public void updateSessionActivity(String sessionId, FirestoreCallback callback) {
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("lastAccessTime", new java.util.Date());
        
        db.collection(COLLECTION_USER_SESSIONS)
            .document(sessionId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Session activity updated");
                callback.onSuccess(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating session activity", e);
                callback.onFailure(e);
            });
    }

    /**
     * Cerrar sesión (marcar como inactiva)
     */
    public void closeUserSession(String sessionId, FirestoreCallback callback) {
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("isActive", false);
        updates.put("logoutTime", new java.util.Date());
        
        db.collection(COLLECTION_USER_SESSIONS)
            .document(sessionId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Session closed");
                callback.onSuccess(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error closing session", e);
                callback.onFailure(e);
            });
    }

    /**
     * Cerrar todas las sesiones activas de un usuario (útil para "cerrar sesión en todos los dispositivos")
     */
    public void closeAllUserSessions(String userId, FirestoreCallback callback) {
        db.collection(COLLECTION_USER_SESSIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("isActive", false);
                    updates.put("logoutTime", new java.util.Date());
                    batch.update(document.getReference(), updates);
                }
                batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "All sessions closed for user: " + userId);
                        callback.onSuccess(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error closing all sessions", e);
                        callback.onFailure(e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error finding sessions to close", e);
                callback.onFailure(e);
            });
    }

    // ==================== CALLBACK ====================

    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onFailure(Exception e);
    }
}

