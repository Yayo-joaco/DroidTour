package com.example.droidtour.managers;


import android.content.Context;
import android.content.SharedPreferences;

/**
 * Variables que se guardan aqi:
 * - Guardar tema claro/oscuro
 * - Guardar usuario logueado
 * - Configuraciones de la app
 * - Tokens de sesión
 * - Preferencias (notificaciones, sonidos, etc.)
 */
public class PrefsManager {
    private static final String PREF_NAME = "DroidTourApp";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    
    // ==================== KEYS CONSTANTS ====================
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_FIRST_TIME = "first_time";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_SOUNDS = "sounds_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_LAST_LOGIN = "last_login";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    
    public PrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
    
    // ==================== MÉTODOS BÁSICOS GENÉRICOS ====================
    
    /**
     * Guardar cualquier string
     */
    public void guardar(String clave, String valor) {
        editor.putString(clave, valor).apply();
    }
    
    /**
     * Leer cualquier string
     */
    public String leer(String clave, String defecto) {
        return sharedPreferences.getString(clave, defecto);
    }
    
    /**
     * Guardar boolean
     */
    public void guardarBool(String clave, boolean valor) {
        editor.putBoolean(clave, valor).apply();
    }
    
    /**
     * Leer boolean
     */
    public boolean leerBool(String clave, boolean defecto) {
        return sharedPreferences.getBoolean(clave, defecto);
    }
    
    /**
     * Guardar entero
     */
    public void guardarInt(String clave, int valor) {
        editor.putInt(clave, valor).apply();
    }
    
    /**
     * Leer entero
     */
    public int leerInt(String clave, int defecto) {
        return sharedPreferences.getInt(clave, defecto);
    }
    
    /**
     * Guardar float
     */
    public void guardarFloat(String clave, float valor) {
        editor.putFloat(clave, valor).apply();
    }
    
    /**
     * Leer float
     */
    public float leerFloat(String clave, float defecto) {
        return sharedPreferences.getFloat(clave, defecto);
    }
    
    /**
     * Limpiar todas las preferencias
     */
    public void limpiarTodo() {
        editor.clear().apply();
    }
    
    /**
     * Eliminar una clave específica
     */
    public void eliminar(String clave) {
        editor.remove(clave).apply();
    }
    
    // ==================== MÉTODOS ESPECÍFICOS DE USUARIO ====================
    
    /**
     * Guardar datos de usuario al hacer login
     */
    public void guardarUsuario(String userId, String name, String email, String userType) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putBoolean(KEY_SESSION_ACTIVE, true);
        editor.apply();
    }
    
    /**
     * Obtener ID de usuario
     */
    public String obtenerUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }
    
    /**
     * Obtener nombre de usuario
     */
    public String obtenerUsuario() {
        return sharedPreferences.getString(KEY_USER_NAME, "Invitado");
    }
    
    /**
     * Obtener email de usuario
     */
    public String obtenerEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, "");
    }
    
    /**
     * Obtener tipo de usuario (GUIDE, CLIENT, ADMIN)
     */
    public String obtenerTipoUsuario() {
        return sharedPreferences.getString(KEY_USER_TYPE, "");
    }
    
    /**
     * Verificar si hay sesión activa
     */
    public boolean sesionActiva() {
        return sharedPreferences.getBoolean(KEY_SESSION_ACTIVE, false);
    }
    
    /**
     * Cerrar sesión
     */
    public void cerrarSesion() {
        editor.putBoolean(KEY_SESSION_ACTIVE, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_TYPE);
        editor.remove(KEY_SESSION_ACTIVE);
        editor.remove(KEY_ACCESS_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);

        // Para remover configuraciones del usuario
        // editor.remove(KEY_NOTIFICATIONS);
        // editor.remove(KEY_SOUNDS);
        // editor.remove(KEY_LANGUAGE);
        // editor.remove(KEY_LAST_LOGIN);
        editor.apply();


    }
    
    // ==================== MÉTODOS DE CONFIGURACIÓN ====================
    
    /**
     * Verificar si es la primera vez que abre la app
     */
    public boolean esPrimeraVez() {
        return sharedPreferences.getBoolean(KEY_FIRST_TIME, true);
    }
    
    /**
     * Marcar que ya no es la primera vez
     */
    public void marcarPrimeraVezCompletada() {
        editor.putBoolean(KEY_FIRST_TIME, false).apply();
    }
    
    /**
     * Guardar preferencia de tema oscuro
     */
    public void guardarTemaOscuro(boolean activo) {
        editor.putBoolean(KEY_DARK_THEME, activo).apply();
    }
    
    /**
     * Verificar si tema oscuro está activo
     */
    public boolean esTemaOscuro() {
        return sharedPreferences.getBoolean(KEY_DARK_THEME, false);
    }
    
    /**
     * Guardar preferencia de notificaciones
     */
    public void guardarNotificaciones(boolean activas) {
        editor.putBoolean(KEY_NOTIFICATIONS, activas).apply();
    }
    
    /**
     * Verificar si notificaciones están activas
     */
    public boolean notificacionesActivas() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
    }
    
    /**
     * Guardar preferencia de sonidos
     */
    public void guardarSonidos(boolean activos) {
        editor.putBoolean(KEY_SOUNDS, activos).apply();
    }
    
    /**
     * Verificar si sonidos están activos
     */
    public boolean sonidosActivos() {
        return sharedPreferences.getBoolean(KEY_SOUNDS, true);
    }
    
    /**
     * Guardar idioma preferido
     */
    public void guardarIdioma(String idioma) {
        editor.putString(KEY_LANGUAGE, idioma).apply();
    }
    
    /**
     * Obtener idioma preferido
     */
    public String obtenerIdioma() {
        return sharedPreferences.getString(KEY_LANGUAGE, "es");
    }
    
    /**
     * Guardar timestamp del último login
     */
    public void guardarUltimoLogin(long timestamp) {
        editor.putLong(KEY_LAST_LOGIN, timestamp).apply();
    }
    
    /**
     * Obtener timestamp del último login
     */
    public long obtenerUltimoLogin() {
        return sharedPreferences.getLong(KEY_LAST_LOGIN, 0);
    }
    
    // ==================== MÉTODOS DE TOKENS ====================
    
    /**
     * Guardar token de acceso
     */
    public void guardarAccessToken(String token) {
        editor.putString(KEY_ACCESS_TOKEN, token).apply();
    }
    
    /**
     * Obtener token de acceso
     */
    public String obtenerAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, "");
    }
    
    /**
     * Guardar token de refresh
     */
    public void guardarRefreshToken(String token) {
        editor.putString(KEY_REFRESH_TOKEN, token).apply();
    }
    
    /**
     * Obtener token de refresh
     */
    public String obtenerRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, "");
    }
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Verificar si existe una clave
     */
    public boolean existeClave(String clave) {
        return sharedPreferences.contains(clave);
    }
    
    /**
     * Obtener todas las claves
     */
    public java.util.Map<String, ?> obtenerTodasLasClaves() {
        return sharedPreferences.getAll();
    }
}

