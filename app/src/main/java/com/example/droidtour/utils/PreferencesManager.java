package com.example.droidtour.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

public class PreferencesManager {
    private static final String PREF_NAME = "DroidTourPreferences";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_TYPE = "user_type"; // GUIDE, CLIENT, ADMIN, SUPERADMIN
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_SESSION_ACTIVE = "session_active"; // Compatibilidad con PrefsManager
    private static final String KEY_GUIDE_APPROVED = "guide_approved";
    private static final String KEY_GUIDE_RATING = "guide_rating";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_FIRST_TIME = "first_time";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_SOUNDS = "sounds_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_LAST_LOGIN = "last_login";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ==================== USER DATA ====================
    
    public void saveUserData(String userId, String name, String email, String phone, String userType) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_SESSION_ACTIVE, true); // Compatibilidad
        editor.apply();
    }
    
    // Método compatible con PrefsManager (sin phone)
    public void guardarUsuario(String userId, String name, String email, String userType) {
        saveUserData(userId, name, email, "", userType);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Usuario");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, "");
    }

    public boolean isLoggedIn() {
        // Verificar ambas claves para compatibilidad
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) || prefs.getBoolean(KEY_SESSION_ACTIVE, false);
    }
    
    // Método compatible con PrefsManager
    public boolean sesionActiva() {
        return isLoggedIn();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
    
    // Método compatible con PrefsManager
    public void cerrarSesion() {
        logout();
    }

    // ==================== GUIDE DATA ====================
    
    public void setGuideApproved(boolean approved) {
        editor.putBoolean(KEY_GUIDE_APPROVED, approved);
        editor.apply();
    }

    public boolean isGuideApproved() {
        return prefs.getBoolean(KEY_GUIDE_APPROVED, false);
    }

    public void setGuideRating(float rating) {
        editor.putFloat(KEY_GUIDE_RATING, rating);
        editor.apply();
    }

    public float getGuideRating() {
        return prefs.getFloat(KEY_GUIDE_RATING, 0.0f);
    }

    // ==================== NOTIFICATIONS ====================
    
    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled);
        editor.apply();
    }

    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
    
    // ==================== MÉTODOS COMPATIBLES CON PrefsManager ====================
    
    // Métodos de usuario (compatibilidad)
    public String obtenerUserId() {
        return getUserId();
    }
    
    public String obtenerUsuario() {
        return getUserName();
    }
    
    public String obtenerEmail() {
        return getUserEmail();
    }
    
    public String obtenerTipoUsuario() {
        return getUserType();
    }
    
    // Métodos de configuración
    public boolean esPrimeraVez() {
        return prefs.getBoolean(KEY_FIRST_TIME, true);
    }
    
    public void marcarPrimeraVezCompletada() {
        editor.putBoolean(KEY_FIRST_TIME, false);
        editor.apply();
    }
    
    public void guardarTemaOscuro(boolean activo) {
        editor.putBoolean(KEY_DARK_THEME, activo);
        editor.apply();
    }
    
    public boolean esTemaOscuro() {
        return prefs.getBoolean(KEY_DARK_THEME, false);
    }
    
    public void guardarNotificaciones(boolean activas) {
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, activas);
        editor.apply();
    }
    
    public boolean notificacionesActivas() {
        return areNotificationsEnabled();
    }
    
    public void guardarSonidos(boolean activos) {
        editor.putBoolean(KEY_SOUNDS, activos);
        editor.apply();
    }
    
    public boolean sonidosActivos() {
        return prefs.getBoolean(KEY_SOUNDS, true);
    }
    
    public void guardarIdioma(String idioma) {
        editor.putString(KEY_LANGUAGE, idioma);
        editor.apply();
    }
    
    public String obtenerIdioma() {
        return prefs.getString(KEY_LANGUAGE, "es");
    }
    
    public void guardarUltimoLogin(long timestamp) {
        editor.putLong(KEY_LAST_LOGIN, timestamp);
        editor.apply();
    }
    
    public long obtenerUltimoLogin() {
        return prefs.getLong(KEY_LAST_LOGIN, 0);
    }
    
    // Métodos de tokens
    public void guardarAccessToken(String token) {
        editor.putString(KEY_ACCESS_TOKEN, token);
        editor.apply();
    }
    
    public String obtenerAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }
    
    public void guardarRefreshToken(String token) {
        editor.putString(KEY_REFRESH_TOKEN, token);
        editor.apply();
    }
    
    public String obtenerRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, "");
    }
    
    // Métodos genéricos
    public void guardar(String clave, String valor) {
        editor.putString(clave, valor);
        editor.apply();
    }
    
    public String leer(String clave, String defecto) {
        return prefs.getString(clave, defecto);
    }
    
    public void guardarBool(String clave, boolean valor) {
        editor.putBoolean(clave, valor);
        editor.apply();
    }
    
    public boolean leerBool(String clave, boolean defecto) {
        return prefs.getBoolean(clave, defecto);
    }
    
    public void guardarInt(String clave, int valor) {
        editor.putInt(clave, valor);
        editor.apply();
    }
    
    public int leerInt(String clave, int defecto) {
        return prefs.getInt(clave, defecto);
    }
    
    public void guardarFloat(String clave, float valor) {
        editor.putFloat(clave, valor);
        editor.apply();
    }
    
    public float leerFloat(String clave, float defecto) {
        return prefs.getFloat(clave, defecto);
    }
    
    public void limpiarTodo() {
        editor.clear();
        editor.apply();
    }
    
    public void eliminar(String clave) {
        editor.remove(clave);
        editor.apply();
    }
    
    public boolean existeClave(String clave) {
        return prefs.contains(clave);
    }
    
    public Map<String, ?> obtenerTodasLasClaves() {
        return prefs.getAll();
    }


    public void clearUserData() {
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_PHONE);
        editor.remove(KEY_USER_TYPE);
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_SESSION_ACTIVE);
        editor.remove(KEY_GUIDE_APPROVED);
        editor.remove(KEY_GUIDE_RATING);
        editor.apply();
    }
}

