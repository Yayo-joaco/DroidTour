package com.example.droidtour;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Clase Application para configuraciones globales de la app
 * Se ejecuta antes de cualquier Activity
 */
public class DroidTourApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Configurar persistencia de Realtime Database ANTES de cualquier uso
        // Esto debe hacerse solo una vez y antes de cualquier llamada a getInstance()
        FirebaseDatabase.getInstance("https://droidtour-default-rtdb.firebaseio.com/")
                .setPersistenceEnabled(true);
    }
}

