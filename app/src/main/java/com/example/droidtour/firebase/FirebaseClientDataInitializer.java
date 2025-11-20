package com.example.droidtour.firebase;

import android.util.Log;

import com.example.droidtour.models.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Inicializador de datos de ejemplo para el módulo CLIENTE en Firebase
 * Similar a DatabaseHelper pero crea datos en Firestore
 */
public class FirebaseClientDataInitializer {
    private static final String TAG = "FirebaseClientInit";
    
    private final FirestoreManager firestoreManager;
    
    // ID del cliente de ejemplo
    private String clientUserId;

    public FirebaseClientDataInitializer() {
        this.firestoreManager = FirestoreManager.getInstance();
    }

    /**
     * Inicializar TODOS los datos de ejemplo para el cliente
     * Esto reemplaza los datos hardcoded de SQLite
     */
    public void initializeAllClientData(ClientDataCallback callback) {
        initializeAllClientData(null, callback);
    }
    
    /**
     * Inicializar datos con un UID específico
     * @param userId UID del usuario de Firebase Authentication (REQUERIDO - usa el UID del usuario actual)
     */
    public void initializeAllClientData(String userId, ClientDataCallback callback) {
        Log.d(TAG, "Iniciando creación de datos de ejemplo para CLIENTE...");
        
        // ⚠️ Validar que se proporcione un userId
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new Exception("Se requiere un UID de usuario válido. No se puede inicializar sin usuario autenticado."));
            return;
        }
        
        clientUserId = userId;
        Log.d(TAG, "🔥 Usando UID del usuario actual: " + clientUserId);
        
        // Paso 1: Crear empresas primero (necesarias para tours)
        createExampleCompanies(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ Empresas creadas");
                
                // Paso 2: Crear tours
                createExampleTours(new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "✅ Tours creados");
                        
                        // Paso 3: NO crear usuario (ya existe) - ir directo a preferencias
                        // Paso 4: Crear preferencias del usuario
                        createExamplePreferences(clientUserId, new FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        Log.d(TAG, "✅ Preferencias creadas");
                                        
                                        // Paso 5: Crear métodos de pago
                                        createExamplePaymentMethods(clientUserId, new FirestoreManager.FirestoreCallback() {
                                            @Override
                                            public void onSuccess(Object result) {
                                                Log.d(TAG, "✅ Métodos de pago creados");
                                                
                                                // Paso 6: Crear reservas
                                                createExampleReservations(clientUserId, new FirestoreManager.FirestoreCallback() {
                                                    @Override
                                                    public void onSuccess(Object result) {
                                                        Log.d(TAG, "✅ Reservas creadas");
                                                        
                                                        // Paso 7: Crear notificaciones
                                                        createExampleNotifications(clientUserId, new FirestoreManager.FirestoreCallback() {
                                                            @Override
                                                            public void onSuccess(Object result) {
                                                                Log.d(TAG, "✅ Notificaciones creadas");
                                                                
                                                                // Paso 8: Crear reseñas
                                                                createExampleReviews(clientUserId, new FirestoreManager.FirestoreCallback() {
                                                                    @Override
                                                                    public void onSuccess(Object result) {
                                                                        Log.d(TAG, "✅ Reseñas creadas");
                                                                        
                                                                        // Paso 9: Crear mensajes de chat
                                                                        createExampleChatMessages(clientUserId, new FirestoreManager.FirestoreCallback() {
                                                                            @Override
                                                                            public void onSuccess(Object result) {
                                                                                Log.d(TAG, "✅ Mensajes de chat creados");
                                                                                Log.d(TAG, "🎉 TODOS los datos de ejemplo creados exitosamente");
                                                                                callback.onSuccess();
                                                                            }
                                                                            
                                                                            @Override
                                                                            public void onFailure(Exception e) {
                                                                                Log.e(TAG, "Error creando mensajes", e);
                                                                                callback.onFailure(e);
                                                                            }
                                                    });
                                                }
                                                
                                                @Override
                                                public void onFailure(Exception e) {
                                                    Log.e(TAG, "Error creando reseñas", e);
                                                    callback.onFailure(e);
                                                }
                                            });
                                        }
                                        
                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e(TAG, "Error creando notificaciones", e);
                                            callback.onFailure(e);
                                        }
                                    });
                                }
                                
                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Error creando reservas", e);
                                    callback.onFailure(e);
                                }
                            });
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error creando métodos de pago", e);
                            callback.onFailure(e);
                        }
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error creando preferencias", e);
                    callback.onFailure(e);
                }
            });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error creando tours", e);
                        callback.onFailure(e);
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error creando empresas", e);
                callback.onFailure(e);
            }
        });
    }

    /**
     * Crear usuario cliente de ejemplo: a20221957@pucp.edu.pe
     */
    private void createExampleClientUser(String existingUserId, FirestoreManager.FirestoreCallback callback) {
        User cliente = new User(
            "a20221957@pucp.edu.pe",
            "Gabrielle Ivonne",
            "Valdivia Matos",
            "+51912345678",
            "DNI",
            "72845123",
            "2002-06-15",
            "Av. Universitaria 1801, San Miguel, Lima, Perú",
            "CLIENT"
        );
        
        // Si se proporciona un UID existente, usarlo
        if (existingUserId != null && !existingUserId.isEmpty()) {
            cliente.setUserId(existingUserId);
            Log.d(TAG, "Usando UID existente: " + existingUserId);
        }
        
        firestoreManager.createUser(cliente, callback);
    }

    /**
     * Crear preferencias del cliente
     */
    private void createExamplePreferences(String userId, FirestoreManager.FirestoreCallback callback) {
        UserPreferences prefs = new UserPreferences(userId);
        prefs.setPushNotificationsEnabled(true);
        prefs.setEmailNotificationsEnabled(false);
        prefs.setPreferredLanguage("ES");
        prefs.setTheme("AUTO");
        prefs.setCurrency("PEN");
        
        firestoreManager.createUserPreferences(prefs, callback);
    }

    /**
     * Crear 2 métodos de pago de ejemplo
     */
    private void createExamplePaymentMethods(String userId, FirestoreManager.FirestoreCallback callback) {
        // 🔥 Obtener el nombre del usuario actual desde Firestore
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                String cardHolderName = user != null && user.getFullName() != null ? 
                                       user.getFullName().toUpperCase() : 
                                       "GERARDO RABANAL";
                
                Log.d(TAG, "Creando payment methods para: " + cardHolderName);
                
                // Tarjeta 1: Visa (Principal)
                PaymentMethod visa = new PaymentMethod(
                    userId,
                    cardHolderName,  // ← Usa el nombre REAL del usuario
                    "4532123456781234", // Se enmascara automáticamente a ****1234
                    "VISA",
                    "12",
                    "2026"
                );
                visa.setIsDefault(true);
                
                firestoreManager.addPaymentMethod(visa, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "✅ Visa creada: " + result);
                        
                        // Tarjeta 2: Mastercard
                        PaymentMethod mastercard = new PaymentMethod(
                            userId,
                            cardHolderName,  // ← Usa el nombre REAL del usuario
                            "5412123456785678", // Se enmascara a ****5678
                            "MASTERCARD",
                            "08",
                            "2025"
                        );
                        mastercard.setIsDefault(false);
                        
                        firestoreManager.addPaymentMethod(mastercard, new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Log.d(TAG, "✅ Mastercard creada: " + result);
                                callback.onSuccess(true);
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "❌ Error creando Mastercard", e);
                                callback.onFailure(e);
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌ Error creando Visa", e);
                        callback.onFailure(e);
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error obteniendo usuario, usando nombre por defecto", e);
                // Fallback: usar nombre por defecto si falla
                createPaymentMethodsWithDefaultName(userId, callback);
            }
        });
    }
    
    /**
     * Fallback: Crear payment methods con nombre por defecto
     */
    private void createPaymentMethodsWithDefaultName(String userId, FirestoreManager.FirestoreCallback callback) {
        PaymentMethod visa = new PaymentMethod(
            userId,
            "CLIENTE EJEMPLO",
            "4532123456781234",
            "VISA",
            "12",
            "2026"
        );
        visa.setIsDefault(true);
        
        firestoreManager.addPaymentMethod(visa, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ Visa creada (fallback)");
                
                PaymentMethod mastercard = new PaymentMethod(
                    userId,
                    "CLIENTE EJEMPLO",
                    "5412123456785678",
                    "MASTERCARD",
                    "08",
                    "2025"
                );
                mastercard.setIsDefault(false);
                
                firestoreManager.addPaymentMethod(mastercard, callback);
            }
            
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Crear 4 reservas de ejemplo (igual que en DatabaseHelper)
     */
    private void createExampleReservations(String userId, FirestoreManager.FirestoreCallback callback) {
        List<Reservation> reservations = new ArrayList<>();
        
        // Reserva 1: City Tour Lima - CONFIRMADA
        Reservation res1 = new Reservation(
            userId,
            "Gabrielle Ivonne Valdivia Matos",
            "a20221957@pucp.edu.pe",
            "TOUR001",
            "City Tour Lima Centro Histórico",
            "COMP001",
            "Lima Adventure Tours",
            "2024-10-28",
            "09:00",
            2,
            85.0
        );
        res1.setStatus("CONFIRMADA");
        res1.setPaymentStatus("CONFIRMADO");
        reservations.add(res1);
        
        // Reserva 2: Machu Picchu - CONFIRMADA
        Reservation res2 = new Reservation(
            userId,
            "Gabrielle Ivonne Valdivia Matos",
            "a20221957@pucp.edu.pe",
            "TOUR002",
            "Machu Picchu Full Day",
            "COMP002",
            "Cusco Explorer",
            "2024-10-30",
            "09:00",
            1,
            350.0
        );
        res2.setStatus("CONFIRMADA");
        res2.setPaymentStatus("CONFIRMADO");
        reservations.add(res2);
        
        // Reserva 3: Islas Ballestas - CONFIRMADA
        Reservation res3 = new Reservation(
            userId,
            "Gabrielle Ivonne Valdivia Matos",
            "a20221957@pucp.edu.pe",
            "TOUR003",
            "Islas Ballestas y Paracas",
            "COMP003",
            "Paracas Tours",
            "2024-11-01",
            "08:00",
            2,
            65.0
        );
        res3.setStatus("CONFIRMADA");
        res3.setPaymentStatus("CONFIRMADO");
        reservations.add(res3);
        
        // Reserva 4: Cañón del Colca - COMPLETADA
        Reservation res4 = new Reservation(
            userId,
            "Gabrielle Ivonne Valdivia Matos",
            "a20221957@pucp.edu.pe",
            "TOUR004",
            "Cañón del Colca 2D/1N",
            "COMP004",
            "Arequipa Adventures",
            "2024-10-15",
            "07:00",
            2,
            180.0
        );
        res4.setStatus("COMPLETADA");
        res4.setPaymentStatus("COBRADO");
        res4.setHasCheckedIn(true);
        res4.setHasCheckedOut(true);
        reservations.add(res4);
        
        // Crear todas las reservas secuencialmente
        createReservationsSequentially(reservations, 0, callback);
    }

    /**
     * Método auxiliar para crear reservas una por una
     */
    private void createReservationsSequentially(List<Reservation> reservations, int index, 
                                               FirestoreManager.FirestoreCallback finalCallback) {
        if (index >= reservations.size()) {
            finalCallback.onSuccess(true);
            return;
        }
        
        Reservation reservation = reservations.get(index);
        firestoreManager.createReservation(reservation, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Reserva " + (index + 1) + " creada: " + reservation.getTourName());
                createReservationsSequentially(reservations, index + 1, finalCallback);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error creando reserva " + (index + 1), e);
                // Continuar con la siguiente aunque falle
                createReservationsSequentially(reservations, index + 1, finalCallback);
            }
        });
    }

    /**
     * Crear 6 notificaciones de ejemplo (igual que en ClientNotificationsActivity)
     */
    private void createExampleNotifications(String userId, FirestoreManager.FirestoreCallback callback) {
        List<Notification> notifications = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        
        // Notificación 1: Reserva Confirmada (más reciente) - NO LEÍDA - IMPORTANTE
        Notification notif1 = new Notification(
            userId,
            "CLIENT",
            Notification.TYPE_RESERVATION_CONFIRMED,
            "Reserva Confirmada",
            "Tu reserva para City Tour Lima Centro Histórico el 28 Oct ha sido confirmada. Código: QR-2024-001",
            "RES001",
            "reservation"
        );
        notif1.setIsRead(false);
        notif1.setIsImportant(true);  // ← IMPORTANTE
        notifications.add(notif1);
        
        // Notificación 2: Pago Confirmado - NO LEÍDA - IMPORTANTE
        Notification notif2 = new Notification(
            userId,
            "CLIENT",
            Notification.TYPE_PAYMENT_CHARGED,
            "Pago Confirmado",
            "Tu pago de S/. 85.0 para City Tour Lima Centro Histórico ha sido procesado exitosamente",
            "RES001",
            "payment"
        );
        notif2.setIsRead(false);
        notif2.setIsImportant(true);  // ← IMPORTANTE
        notifications.add(notif2);
        
        // Notificación 3: Recordatorio de Tour - LEÍDA - IMPORTANTE
        Notification notif3 = new Notification(
            userId,
            "CLIENT",
            Notification.TYPE_TOUR_REMINDER,
            "Recordatorio de Tour",
            "No olvides tu tour Machu Picchu Full Day el 30 Oct a las 09:00 AM",
            "RES002",
            "reservation"
        );
        notif3.setIsRead(true);
        notif3.setIsImportant(true);  // ← IMPORTANTE
        notif3.setReadAt(new Date());
        notifications.add(notif3);
        
        // Notificación 4: Tour Completado - LEÍDA - NO importante
        Notification notif4 = new Notification(
            userId,
            "CLIENT",
            Notification.TYPE_TOUR_COMPLETED,
            "Tour Completado",
            "¡Esperamos que hayas disfrutado City Tour Lima Centro Histórico! ¿Quieres calificarlo?",
            "RES001",
            "reservation"
        );
        notif4.setIsRead(true);
        notif4.setIsImportant(false);  // ← NO importante
        notif4.setReadAt(new Date());
        notifications.add(notif4);
        
        // Notificación 5: Reserva Confirmada (Islas Ballestas) - LEÍDA - NO importante
        Notification notif5 = new Notification(
            userId,
            "CLIENT",
            Notification.TYPE_RESERVATION_CONFIRMED,
            "Reserva Confirmada",
            "Tu reserva para Islas Ballestas y Paracas el 1 Nov ha sido confirmada. Código: QR-2024-002",
            "RES003",
            "reservation"
        );
        notif5.setIsRead(true);
        notif5.setIsImportant(false);  // ← NO importante
        notif5.setReadAt(new Date());
        notifications.add(notif5);
        
        // Notificación 6: Pago Confirmado (Islas Ballestas) - LEÍDA - NO importante
        Notification notif6 = new Notification(
            userId,
            "CLIENT",
            Notification.TYPE_PAYMENT_CHARGED,
            "Pago Confirmado",
            "Tu pago de S/. 65.0 para Islas Ballestas y Paracas ha sido procesado exitosamente",
            "RES003",
            "payment"
        );
        notif6.setIsRead(true);
        notif6.setIsImportant(false);  // ← NO importante
        notif6.setReadAt(new Date());
        notifications.add(notif6);
        
        // Crear todas las notificaciones secuencialmente
        createNotificationsSequentially(notifications, 0, callback);
    }

    /**
     * Método auxiliar para crear notificaciones una por una
     */
    private void createNotificationsSequentially(List<Notification> notifications, int index,
                                                FirestoreManager.FirestoreCallback finalCallback) {
        if (index >= notifications.size()) {
            finalCallback.onSuccess(true);
            return;
        }
        
        Notification notification = notifications.get(index);
        firestoreManager.createNotification(notification, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Notificación " + (index + 1) + " creada: " + notification.getTitle());
                createNotificationsSequentially(notifications, index + 1, finalCallback);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error creando notificación " + (index + 1), e);
                // Continuar con la siguiente aunque falle
                createNotificationsSequentially(notifications, index + 1, finalCallback);
            }
        });
    }

    /**
     * Crear empresas de ejemplo
     */
    private void createExampleCompanies(FirestoreManager.FirestoreCallback callback) {
        List<Company> companies = new ArrayList<>();
        
        // Empresa 1: Lima Adventure Tours (1 tour)
        Company comp1 = new Company("Lima Adventure Tours", "ADMIN001", "info@limaadventure.com", "+51987654321", "Lima", "Perú");
        comp1.setCompanyId("COMP001");
        comp1.setAddress("Av. José Larco 123, Miraflores, Lima, Perú");
        comp1.setAverageRating(4.8);
        comp1.setTotalReviews(245);
        comp1.setTotalTours(1); // ✅ Corregido: solo tiene 1 tour
        comp1.setActive(true);
        companies.add(comp1);
        
        // Empresa 2: Cusco Explorer (2 tours)
        Company comp2 = new Company("Cusco Explorer", "ADMIN002", "info@cuscoexplorer.com", "+51984123456", "Cusco", "Perú");
        comp2.setCompanyId("COMP002");
        comp2.setAddress("Plaza de Armas 456, Cusco, Perú");
        comp2.setAverageRating(4.9);
        comp2.setTotalReviews(189);
        comp2.setTotalTours(2); // ✅ Corregido: tiene 2 tours (Machu Picchu + Valle Sagrado)
        comp2.setActive(true);
        companies.add(comp2);
        
        // Empresa 3: Paracas Tours (1 tour)
        Company comp3 = new Company("Paracas Tours", "ADMIN003", "info@paracas-tours.com", "+51956789012", "Paracas", "Perú");
        comp3.setCompanyId("COMP003");
        comp3.setAddress("Av. Paracas 789, Paracas, Ica, Perú");
        comp3.setAverageRating(4.7);
        comp3.setTotalReviews(156);
        comp3.setTotalTours(1); // ✅ Corregido: solo tiene 1 tour
        comp3.setActive(true);
        companies.add(comp3);
        
        // Crear todas las empresas secuencialmente
        createCompaniesSequentially(companies, 0, callback);
    }
    
    private void createCompaniesSequentially(List<Company> companies, int index, FirestoreManager.FirestoreCallback finalCallback) {
        if (index >= companies.size()) {
            finalCallback.onSuccess(true);
            return;
        }
        
        Company company = companies.get(index);
        
        // 🔥 Crear con ID específico para que los tours puedan referenciarla correctamente
        String companyId = company.getCompanyId();
        if (companyId != null && !companyId.isEmpty()) {
            // Usar el ID que ya tiene asignado
            firestoreManager.createCompanyWithId(companyId, company, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "Empresa " + (index + 1) + " creada: " + company.getCompanyName() + " con ID: " + companyId);
                    createCompaniesSequentially(companies, index + 1, finalCallback);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error creando empresa " + (index + 1), e);
                    createCompaniesSequentially(companies, index + 1, finalCallback);
                }
            });
        } else {
            // Si no tiene ID, usar el método normal
            firestoreManager.createCompany(company, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "Empresa " + (index + 1) + " creada: " + company.getCompanyName());
                    createCompaniesSequentially(companies, index + 1, finalCallback);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error creando empresa " + (index + 1), e);
                    createCompaniesSequentially(companies, index + 1, finalCallback);
                }
            });
        }
    }

    /**
     * Crear tours de ejemplo
     */
    private void createExampleTours(FirestoreManager.FirestoreCallback callback) {
        List<Tour> tours = new ArrayList<>();
        
        // Tour 1: City Tour Lima Centro Histórico
        Tour tour1 = new Tour("City Tour Lima Centro Histórico", "COMP001", "Lima Adventure Tours", 
            "Descubre la historia y cultura de Lima visitando sus principales atractivos del centro histórico. Un recorrido completo por la Plaza de Armas, Catedral, Palacio de Gobierno y los balcones coloniales más emblemáticos.",
            85.0, "4 horas", 12);
        tour1.setTourId("TOUR001");
        tour1.setLanguages(Arrays.asList("ES", "EN"));
        tour1.setIncludedServices(Arrays.asList("Transporte", "Guía profesional", "Entradas"));
        tour1.setAverageRating(4.9);
        tour1.setTotalReviews(127);
        tour1.setActive(true);
        tours.add(tour1);
        
        // Tour 2: Machu Picchu Full Day
        Tour tour2 = new Tour("Machu Picchu Full Day", "COMP002", "Cusco Explorer",
            "Visita la maravilla del mundo con transporte y guía incluido desde Cusco. Una experiencia única que incluye tren panorámico y tiempo suficiente para explorar la ciudadela inca.",
            350.0, "Full Day", 15);
        tour2.setTourId("TOUR002");
        tour2.setLanguages(Arrays.asList("ES", "EN", "FR"));
        tour2.setIncludedServices(Arrays.asList("Tren panorámico", "Guía experto", "Almuerzo", "Entradas"));
        tour2.setAverageRating(4.9);
        tour2.setTotalReviews(189);
        tour2.setActive(true);
        tours.add(tour2);
        
        // Tour 3: Islas Ballestas y Paracas
        Tour tour3 = new Tour("Islas Ballestas y Paracas", "COMP003", "Paracas Tours",
            "Disfruta de un paseo en lancha por las Islas Ballestas para observar lobos marinos, pingüinos y aves guaneras. Incluye visita a la Reserva Nacional de Paracas.",
            65.0, "6 horas", 20);
        tour3.setTourId("TOUR003");
        tour3.setLanguages(Arrays.asList("ES", "EN"));
        tour3.setIncludedServices(Arrays.asList("Transporte", "Lancha", "Guía", "Desayuno"));
        tour3.setAverageRating(4.7);
        tour3.setTotalReviews(95);
        tour3.setActive(true);
        tours.add(tour3);
        
        // Tour 4: Valle Sagrado Cusco
        Tour tour4 = new Tour("Valle Sagrado Cusco", "COMP002", "Cusco Explorer",
            "Explora el Valle Sagrado de los Incas visitando Pisac, Ollantaytambo y Chinchero. Incluye almuerzo buffet y tiempo libre en cada sitio arqueológico.",
            120.0, "Full Day", 18);
        tour4.setTourId("TOUR004");
        tour4.setLanguages(Arrays.asList("ES", "EN"));
        tour4.setIncludedServices(Arrays.asList("Transporte", "Guía", "Almuerzo", "Entradas"));
        tour4.setAverageRating(4.8);
        tour4.setTotalReviews(78);
        tour4.setActive(true);
        tours.add(tour4);
        
        // Crear todos los tours secuencialmente
        createToursSequentially(tours, 0, callback);
    }
    
    private void createToursSequentially(List<Tour> tours, int index, FirestoreManager.FirestoreCallback finalCallback) {
        if (index >= tours.size()) {
            finalCallback.onSuccess(true);
            return;
        }
        
        Tour tour = tours.get(index);
        
        // 🔥 Crear con ID específico para mantener referencias consistentes
        String tourId = tour.getTourId();
        if (tourId != null && !tourId.isEmpty()) {
            // Usar el ID que ya tiene asignado
            firestoreManager.createTourWithId(tourId, tour, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "Tour " + (index + 1) + " creado: " + tour.getTourName() + " con ID: " + tourId);
                    createToursSequentially(tours, index + 1, finalCallback);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error creando tour " + (index + 1), e);
                    createToursSequentially(tours, index + 1, finalCallback);
                }
            });
        } else {
            // Si no tiene ID, usar el método normal
            firestoreManager.createTour(tour, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "Tour " + (index + 1) + " creado: " + tour.getTourName());
                    createToursSequentially(tours, index + 1, finalCallback);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error creando tour " + (index + 1), e);
                    createToursSequentially(tours, index + 1, finalCallback);
                }
            });
        }
    }

    /**
     * Crear reseñas de ejemplo
     */
    private void createExampleReviews(String userId, FirestoreManager.FirestoreCallback callback) {
        List<Review> reviews = new ArrayList<>();
        
        // Reseñas para City Tour Lima
        Review rev1 = new Review(userId, "Juan Pérez", "J", "TOUR001", 
            "City Tour Lima Centro Histórico", "COMP001", "Lima Adventure Tours", "RES002",
            5.0f, "Excelente tour, el guía muy conocedor y amable. Los lugares visitados fueron increíbles y la comida deliciosa.", "Tour increíble");
        reviews.add(rev1);
        
        Review rev2 = new Review("USER_OTHER_1", "Ana García", "A", "TOUR001",
            "City Tour Lima Centro Histórico", "COMP001", "Lima Adventure Tours", "RES_OTHER_1",
            5.0f, "Una experiencia inolvidable. La organización fue perfecta y aprendimos mucho sobre la historia de Lima.", "Inolvidable");
        reviews.add(rev2);
        
        Review rev3 = new Review("USER_OTHER_2", "Carlos Mendoza", "C", "TOUR001",
            "City Tour Lima Centro Histórico", "COMP001", "Lima Adventure Tours", "RES_OTHER_2",
            4.0f, "Muy recomendado. El tour cumplió todas nuestras expectativas y el precio es muy justo.", "Muy recomendado");
        reviews.add(rev3);
        
        // Crear todas las reseñas secuencialmente
        createReviewsSequentially(reviews, 0, callback);
    }
    
    private void createReviewsSequentially(List<Review> reviews, int index, FirestoreManager.FirestoreCallback finalCallback) {
        if (index >= reviews.size()) {
            finalCallback.onSuccess(true);
            return;
        }
        
        Review review = reviews.get(index);
        firestoreManager.createReview(review, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Reseña " + (index + 1) + " creada");
                createReviewsSequentially(reviews, index + 1, finalCallback);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error creando reseña " + (index + 1), e);
                createReviewsSequentially(reviews, index + 1, finalCallback);
            }
        });
    }

    /**
     * Crear mensajes de chat de ejemplo
     */
    private void createExampleChatMessages(String userId, FirestoreManager.FirestoreCallback callback) {
        // Conversación con Lima Adventure Tours (COMP001)
        String conversationId1 = userId + "_COMP001";
        
        // Mensaje 1: De la empresa al cliente
        firestoreManager.sendMessage(
            "COMP001", 
            "Lima Adventure Tours",
            userId,
            "Gabrielle Valdivia",
            "COMPANY",
            "¡Hola! Gracias por tu interés en nuestros tours. ¿En qué podemos ayudarte?",
            conversationId1,
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "Mensaje 1 creado");
                    
                    // Mensaje 2: Del cliente a la empresa
                    firestoreManager.sendMessage(
                        userId,
                        "Gabrielle Valdivia",
                        "COMP001",
                        "Lima Adventure Tours",
                        "CLIENT",
                        "Hola, me gustaría información sobre el City Tour Lima Centro Histórico",
                        conversationId1,
                        new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Log.d(TAG, "Mensaje 2 creado");
                                
                                // Mensaje 3: De la empresa al cliente
                                firestoreManager.sendMessage(
                                    "COMP001",
                                    "Lima Adventure Tours",
                                    userId,
                                    "Gabrielle Valdivia",
                                    "COMPANY",
                                    "¡Claro! El City Tour incluye visita a la Plaza Mayor, Catedral de Lima, Convento de San Francisco y más. Duración: 4 horas. Precio: S/. 85 por persona. ¿Te gustaría reservar?",
                                    conversationId1,
                                    new FirestoreManager.FirestoreCallback() {
                                        @Override
                                        public void onSuccess(Object result) {
                                            Log.d(TAG, "Mensaje 3 creado");
                                            
                                            // Mensaje 4: Del cliente
                                            firestoreManager.sendMessage(
                                                userId,
                                                "Gabrielle Valdivia",
                                                "COMP001",
                                                "Lima Adventure Tours",
                                                "CLIENT",
                                                "Perfecto, ¿incluye almuerzo?",
                                                conversationId1,
                                                new FirestoreManager.FirestoreCallback() {
                                                    @Override
                                                    public void onSuccess(Object result) {
                                                        Log.d(TAG, "Mensaje 4 creado");
                                                        
                                                        // Mensaje 5: De la empresa
                                                        firestoreManager.sendMessage(
                                                            "COMP001",
                                                            "Lima Adventure Tours",
                                                            userId,
                                                            "Gabrielle Valdivia",
                                                            "COMPANY",
                                                            "Sí, incluye almuerzo en restaurante local del centro histórico. También incluye transporte y guía bilingüe.",
                                                            conversationId1,
                                                            callback
                                                        );
                                                    }
                                                    
                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        callback.onFailure(e);
                                                    }
                                                }
                                            );
                                        }
                                        
                                        @Override
                                        public void onFailure(Exception e) {
                                            callback.onFailure(e);
                                        }
                                    }
                                );
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        }
                    );
                }
                
                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            }
        );
    }

    // Callback para notificar cuando se complete la inicialización
    public interface ClientDataCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}

