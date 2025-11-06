package com.example.droidtour.firebase;

import android.content.Context;
import android.util.Log;

import com.example.droidtour.models.Company;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.User;

import java.util.Arrays;

/**
 * Clase de utilidad para inicializar datos de ejemplo en Firebase
 * SOLO USAR EN DESARROLLO - NO EN PRODUCCIÓN
 */
public class FirebaseInitializer {
    private static final String TAG = "FirebaseInitializer";
    
    private final FirestoreManager firestoreManager;

    public FirebaseInitializer() {
        this.firestoreManager = FirestoreManager.getInstance();
    }

    /**
     * Crear datos de ejemplo en Firestore
     * Llamar este método SOLO UNA VEZ durante el desarrollo
     */
    public void createSampleData(SampleDataCallback callback) {
        // Crear empresa de ejemplo
        createSampleCompany(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                String companyId = (String) result;
                Log.d(TAG, "Sample company created: " + companyId);
                
                // Crear tours de ejemplo para esta empresa
                createSampleTours(companyId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "Sample tours created");
                        callback.onSuccess("Datos de ejemplo creados exitosamente");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error creating sample tours", e);
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error creating sample company", e);
                callback.onFailure(e);
            }
        });
    }

    /**
     * Crear empresa de ejemplo
     */
    private void createSampleCompany(FirestoreManager.FirestoreCallback callback) {
        Company company = new Company(
            "Lima Adventure Tours",
            "ADMIN_USER_ID", // Reemplazar con ID real
            "info@limaadventure.com",
            "+51987654321",
            "Lima",
            "Perú"
        );
        
        company.setDescription("Empresa líder en tours por Lima y alrededores. Más de 10 años de experiencia ofreciendo las mejores experiencias turísticas.");
        company.setRuc("20123456789");
        company.setBusinessType("S.A.C.");
        company.setAddress("Av. Arequipa 1234, Miraflores");
        company.setAverageRating(4.8);
        company.setTotalReviews(245);
        company.setTotalTours(12);
        company.setTotalClients(1500);
        company.setPriceFrom(65.0);
        company.setActive(true);
        company.setVerified(true);
        
        firestoreManager.createCompany(company, callback);
    }

    /**
     * Crear tours de ejemplo
     */
    private void createSampleTours(String companyId, FirestoreManager.FirestoreCallback callback) {
        // Tour 1: City Tour Lima
        Tour tour1 = new Tour(
            "City Tour Lima Centro Histórico",
            companyId,
            "Lima Adventure Tours",
            "Descubre la historia y cultura de Lima visitando sus principales atractivos del centro histórico. Un recorrido completo por la Plaza de Armas, Catedral, Palacio de Gobierno y los balcones coloniales más emblemáticos.",
            85.0,
            "4 horas",
            12
        );
        
        tour1.setCategory("Cultural");
        tour1.setLanguages(Arrays.asList("ES", "EN"));
        tour1.setIncludedServices(Arrays.asList("Guía profesional", "Transporte", "Entradas a museos"));
        tour1.setNotIncludedServices(Arrays.asList("Almuerzo", "Propinas"));
        tour1.setMeetingPoint("Plaza San Martín");
        tour1.setDepartureTime("09:00 AM");
        tour1.setMainImageUrl("https://www.dicasdeviagem.com/wp-content/uploads/2020/03/lima-costa-mar-2048x1364.jpg");
        tour1.setAverageRating(4.8);
        tour1.setTotalReviews(127);
        tour1.setTotalBookings(450);
        tour1.setActive(true);
        tour1.setFeatured(true);
        
        // Itinerario del tour 1
        tour1.setItinerary(Arrays.asList(
            new Tour.ItineraryPoint("09:00", "Plaza de Armas", "Visita a la plaza principal de Lima, conoce su historia y arquitectura colonial", "45 minutos"),
            new Tour.ItineraryPoint("10:00", "Catedral de Lima", "Recorrido por el interior de la catedral, admirando su arquitectura y arte religioso", "30 minutos"),
            new Tour.ItineraryPoint("11:30", "Palacio de Gobierno", "Vista exterior del palacio presidencial y cambio de guardia", "45 minutos"),
            new Tour.ItineraryPoint("13:00", "Balcones Coloniales", "Caminata por las calles históricas observando los famosos balcones limeños", "30 minutos")
        ));
        
        firestoreManager.createTour(tour1, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Tour 1 created: " + result);
                
                // Tour 2: Islas Ballestas
                Tour tour2 = new Tour(
                    "Islas Ballestas y Paracas",
                    companyId,
                    "Lima Adventure Tours",
                    "Excursión a las hermosas Islas Ballestas, conocidas como las 'Galápagos del Perú'. Observa lobos marinos, pingüinos de Humboldt y una gran variedad de aves marinas en su hábitat natural.",
                    65.0,
                    "Full Day",
                    15
                );
                
                tour2.setCategory("Naturaleza");
                tour2.setLanguages(Arrays.asList("ES", "EN"));
                tour2.setIncludedServices(Arrays.asList("Transporte ida y vuelta", "Paseo en bote", "Guía naturalista"));
                tour2.setNotIncludedServices(Arrays.asList("Almuerzo", "Bebidas"));
                tour2.setMeetingPoint("Terminal de buses San Isidro");
                tour2.setDepartureTime("06:00 AM");
                tour2.setMainImageUrl("https://image.jimcdn.com/app/cms/image/transf/none/path/s336fd9bc7dca3ebc/image/ida0ff171f4a6d885/version/1391479285/image.jpg");
                tour2.setAverageRating(4.7);
                tour2.setTotalReviews(89);
                tour2.setTotalBookings(320);
                tour2.setActive(true);
                tour2.setFeatured(true);
                
                firestoreManager.createTour(tour2, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "Tour 2 created: " + result);
                        callback.onSuccess("Tours created successfully");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Verificar si Firebase está correctamente configurado
     */
    public static boolean isFirebaseConfigured() {
        try {
            FirebaseAuthManager.getInstance(null);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Firebase not configured", e);
            return false;
        }
    }

    // Callback para creación de datos de ejemplo
    public interface SampleDataCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }
}

