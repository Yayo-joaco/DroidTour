package com.example.droidtour.firebase;

import android.util.Log;
import com.example.droidtour.models.Notification;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.models.TourOffer;

import java.util.Date;
import java.util.Arrays;

/**
 * Inicializador de datos de prueba para guía de turismo
 * NO crea usuarios, solo datos estáticos (ofertas, reservas, notificaciones)
 */
public class FirebaseGuideDataInitializer {
    private static final String TAG = "GuideDataInitializer";
    private final FirestoreManager firestoreManager;

    public FirebaseGuideDataInitializer() {
        this.firestoreManager = FirestoreManager.getInstance();
    }

    /**
     * Inicializar todos los datos de prueba para el guía
     */
    public void initializeAllData(String guideId, String guideName, OnInitializationCompleteListener listener) {
        Log.d(TAG, "🚀 Iniciando inicialización de datos para guía: " + guideId);
        
        // Crear ofertas de tours
        createExampleTourOffers(guideId, guideName, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ Ofertas creadas");
                
                // Crear reservas donde el guía está asignado
                createExampleReservations(guideId, guideName, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "✅ Reservas creadas");
                        
                        // Crear notificaciones para el guía
                        createExampleNotifications(guideId, new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Log.d(TAG, "✅ Notificaciones creadas");
                                Log.d(TAG, "🎉 Inicialización completada exitosamente");
                                listener.onComplete(true, "Datos de guía inicializados correctamente");
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "❌ Error creando notificaciones", e);
                                listener.onComplete(false, "Error creando notificaciones: " + e.getMessage());
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌ Error creando reservas", e);
                        listener.onComplete(false, "Error creando reservas: " + e.getMessage());
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error creando ofertas", e);
                listener.onComplete(false, "Error creando ofertas: " + e.getMessage());
            }
        });
    }

    /**
     * Crear ofertas de tours de ejemplo
     */
    private void createExampleTourOffers(String guideId, String guideName, FirestoreManager.FirestoreCallback callback) {
        Log.d(TAG, "📋 Creando ofertas de tours...");
        
        // Oferta 1: City Tour Lima (Pendiente)
        TourOffer offer1 = new TourOffer(
            guideId,
            guideName,
            "COMP001",
            "Lima Tours SAC",
            "TOUR001",
            "City Tour Lima Centro Histórico",
            "2024-12-20",
            "09:00",
            "4 horas",
            180.0,
            6
        );
        offer1.setStatus("PENDIENTE");
        offer1.setAdditionalNotes("Incluye transporte y guía bilingüe");
        
        // Oferta 2: Cusco Mágico (Pendiente)
        TourOffer offer2 = new TourOffer(
            guideId,
            guideName,
            "COMP002",
            "Cusco Adventure",
            "TOUR002",
            "Cusco Mágico Full Day",
            "2024-12-22",
            "08:00",
            "8 horas",
            350.0,
            12
        );
        offer2.setStatus("PENDIENTE");
        offer2.setAdditionalNotes("Incluye almuerzo y entradas");
        
        // Oferta 3: Paracas (Rechazada)
        TourOffer offer3 = new TourOffer(
            guideId,
            guideName,
            "COMP003",
            "Paracas Explorers",
            "TOUR003",
            "Paracas y Huacachina",
            "2024-12-15",
            "07:00",
            "12 horas",
            450.0,
            8
        );
        offer3.setStatus("RECHAZADA");
        offer3.setRespondedAt(new Date());
        
        // Crear en Firestore
        final int[] count = {0};
        final int total = 3;
        
        firestoreManager.createTourOffer(offer1, createCounterCallback(count, total, callback));
        firestoreManager.createTourOffer(offer2, createCounterCallback(count, total, callback));
        firestoreManager.createTourOffer(offer3, createCounterCallback(count, total, callback));
    }

    /**
     * Crear reservas de ejemplo donde el guía está asignado
     */
    private void createExampleReservations(String guideId, String guideName, FirestoreManager.FirestoreCallback callback) {
        Log.d(TAG, "📅 Creando reservas asignadas al guía...");
        
        // Reserva 1: En progreso
        Reservation reservation1 = new Reservation(
            "CLIENT001",
            "María López",
            "maria@example.com",
            "TOUR001",
            "City Tour Lima Centro Histórico",
            "COMP001",
            "Lima Tours SAC",
            "Hoy",
            "09:00",
            6,
            180.0
        );
        reservation1.setGuideId(guideId);
        reservation1.setGuideName(guideName);
        reservation1.setStatus("EN_PROGRESO");
        reservation1.setPaymentStatus("CONFIRMADO");
        
        // Reserva 2: Programada
        Reservation reservation2 = new Reservation(
            "CLIENT002",
            "Juan Pérez",
            "juan@example.com",
            "TOUR002",
            "Cusco Mágico Full Day",
            "COMP002",
            "Cusco Adventure",
            "Mañana",
            "08:00",
            12,
            350.0
        );
        reservation2.setGuideId(guideId);
        reservation2.setGuideName(guideName);
        reservation2.setStatus("CONFIRMADA");
        reservation2.setPaymentStatus("CONFIRMADO");
        
        // Reserva 3: Completada
        Reservation reservation3 = new Reservation(
            "CLIENT003",
            "Ana García",
            "ana@example.com",
            "TOUR003",
            "Paracas y Huacachina",
            "COMP003",
            "Paracas Explorers",
            "2024-12-10",
            "07:00",
            8,
            450.0
        );
        reservation3.setGuideId(guideId);
        reservation3.setGuideName(guideName);
        reservation3.setStatus("COMPLETADA");
        reservation3.setPaymentStatus("CONFIRMADO");
        
        // Crear en Firestore
        final int[] count = {0};
        final int total = 3;
        
        firestoreManager.createReservation(reservation1, createCounterCallback(count, total, callback));
        firestoreManager.createReservation(reservation2, createCounterCallback(count, total, callback));
        firestoreManager.createReservation(reservation3, createCounterCallback(count, total, callback));
    }

    /**
     * Crear notificaciones de ejemplo para el guía
     */
    private void createExampleNotifications(String guideId, FirestoreManager.FirestoreCallback callback) {
        Log.d(TAG, "🔔 Creando notificaciones para el guía...");
        
        // Notificación 1: Nueva oferta
        Notification notif1 = new Notification();
        notif1.setUserId(guideId);
        notif1.setUserType("GUIDE");
        notif1.setType("TOUR_OFFER");
        notif1.setTitle("Nueva Oferta de Tour");
        notif1.setMessage("Lima Tours SAC te ha enviado una oferta para el City Tour Lima Centro Histórico el 20 de diciembre");
        notif1.setIsRead(false);
        notif1.setIsImportant(true);
        notif1.setCreatedAt(new Date(System.currentTimeMillis() - 3600000)); // Hace 1 hora
        
        // Notificación 2: Tour próximo
        Notification notif2 = new Notification();
        notif2.setUserId(guideId);
        notif2.setUserType("GUIDE");
        notif2.setType("TOUR_REMINDER");
        notif2.setTitle("Tour Próximo");
        notif2.setMessage("Tienes un tour programado para mañana: Cusco Mágico Full Day a las 08:00");
        notif2.setIsRead(false);
        notif2.setIsImportant(true);
        notif2.setCreatedAt(new Date(System.currentTimeMillis() - 7200000)); // Hace 2 horas
        
        // Notificación 3: Tour completado
        Notification notif3 = new Notification();
        notif3.setUserId(guideId);
        notif3.setUserType("GUIDE");
        notif3.setType("TOUR_COMPLETED");
        notif3.setTitle("Tour Completado");
        notif3.setMessage("Has completado exitosamente el tour Paracas y Huacachina. ¡Buen trabajo!");
        notif3.setIsRead(true);
        notif3.setIsImportant(false);
        notif3.setCreatedAt(new Date(System.currentTimeMillis() - 86400000)); // Hace 1 día
        
        // Notificación 4: Pago recibido
        Notification notif4 = new Notification();
        notif4.setUserId(guideId);
        notif4.setUserType("GUIDE");
        notif4.setType("PAYMENT_RECEIVED");
        notif4.setTitle("Pago Recibido");
        notif4.setMessage("Has recibido S/. 450.00 por el tour Paracas y Huacachina");
        notif4.setIsRead(true);
        notif4.setIsImportant(false);
        notif4.setCreatedAt(new Date(System.currentTimeMillis() - 172800000)); // Hace 2 días
        
        // Crear en Firestore
        final int[] count = {0};
        final int total = 4;
        
        firestoreManager.createNotification(notif1, createCounterCallback(count, total, callback));
        firestoreManager.createNotification(notif2, createCounterCallback(count, total, callback));
        firestoreManager.createNotification(notif3, createCounterCallback(count, total, callback));
        firestoreManager.createNotification(notif4, createCounterCallback(count, total, callback));
    }

    /**
     * Crear callback que cuenta cuántas operaciones se completaron
     */
    private FirestoreManager.FirestoreCallback createCounterCallback(int[] count, int total, FirestoreManager.FirestoreCallback finalCallback) {
        return new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                count[0]++;
                if (count[0] == total) {
                    finalCallback.onSuccess(true);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                finalCallback.onFailure(e);
            }
        };
    }

    /**
     * Interfaz para notificar cuando la inicialización se completa
     */
    public interface OnInitializationCompleteListener {
        void onComplete(boolean success, String message);
    }
}

