package com.example.droidtour.managers;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * FileManager - Archivos internos para datos estructurados, cache, información importante
 * 
 */
public class FileManager {
    private static final String TAG = "FileManager";
    private Context context;
    
    // ==================== CONSTANTS ====================
    private static final String CACHE_DIR = "cache";
    private static final String USER_DATA_DIR = "user_data";
    private static final String BACKUP_DIR = "backup";
    private static final String CONFIG_DIR = "config";
    private static final String ARCHIVO_DATOS_USUARIO = "user_data.json";
    
    public FileManager(Context context) {
        this.context = context;
    }
    
    // ==================== MÉTODOS BÁSICOS DE ARCHIVOS ====================
    
    /**
     * Escribir string a archivo interno
     */
    public boolean escribirArchivo(String nombreArchivo, String contenido) {
        try {
            FileOutputStream fos = context.openFileOutput(nombreArchivo, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            writer.write(contenido);
            writer.close();
            fos.close();
            Log.d(TAG, "Archivo escrito: " + nombreArchivo);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error escribiendo archivo: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Leer string de archivo interno
     */
    public String leerArchivo(String nombreArchivo) {
        try {
            FileInputStream fis = context.openFileInput(nombreArchivo);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder contenido = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
            reader.close();
            fis.close();
            Log.d(TAG, "Archivo leído: " + nombreArchivo);
            return contenido.toString().trim();
        } catch (IOException e) {
            Log.e(TAG, "Error leyendo archivo: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Verificar si archivo existe
     */
    public boolean archivoExiste(String nombreArchivo) {
        File archivo = new File(context.getFilesDir(), nombreArchivo);
        return archivo.exists();
    }
    
    /**
     * Eliminar archivo
     */
    public boolean eliminarArchivo(String nombreArchivo) {
        File archivo = new File(context.getFilesDir(), nombreArchivo);
        boolean eliminado = archivo.delete();
        if (eliminado) {
            Log.d(TAG, "Archivo eliminado: " + nombreArchivo);
        } else {
            Log.w(TAG, "No se pudo eliminar archivo: " + nombreArchivo);
        }
        return eliminado;
    }
    
    /**
     * Obtener tamaño de archivo
     */
    public long obtenerTamañoArchivo(String nombreArchivo) {
        File archivo = new File(context.getFilesDir(), nombreArchivo);
        return archivo.length();
    }
    
    // ==================== MÉTODOS DE JSON ====================
    
    /**
     * Guardar JSONObject a archivo
     */
    public boolean guardarJSON(String nombreArchivo, JSONObject json) {
        return escribirArchivo(nombreArchivo, json.toString());
    }
    
    /**
     * Leer JSONObject de archivo
     */
    public JSONObject leerJSON(String nombreArchivo) {
        String contenido = leerArchivo(nombreArchivo);
        if (contenido.isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(contenido);
        } catch (JSONException e) {
            Log.e(TAG, "Error parseando JSON: " + e.getMessage());
            return new JSONObject();
        }
    }
    
    /**
     * Guardar JSONArray a archivo
     */
    public boolean guardarJSONArray(String nombreArchivo, JSONArray jsonArray) {
        return escribirArchivo(nombreArchivo, jsonArray.toString());
    }
    
    /**
     * Leer JSONArray de archivo
     */
    public JSONArray leerJSONArray(String nombreArchivo) {
        String contenido = leerArchivo(nombreArchivo);
        if (contenido.isEmpty()) {
            return new JSONArray();
        }
        try {
            return new JSONArray(contenido);
        } catch (JSONException e) {
            Log.e(TAG, "Error parseando JSONArray: " + e.getMessage());
            return new JSONArray();
        }
    }
    
    // ==================== MÉTODOS DE CACHE ====================
    
    /**
     * Guardar datos de cache con timestamp
     */
    public boolean guardarCache(String clave, String datos) {
        try {
            JSONObject cacheData = new JSONObject();
            cacheData.put("timestamp", System.currentTimeMillis());
            cacheData.put("data", datos);
            return guardarJSON("cache_" + clave + ".json", cacheData);
        } catch (JSONException e) {
            Log.e(TAG, "Error creando cache: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Leer datos de cache
     */
    public String leerCache(String clave) {
        JSONObject cacheData = leerJSON("cache_" + clave + ".json");
        try {
            return cacheData.getString("data");
        } catch (JSONException e) {
            Log.e(TAG, "Error leyendo cache: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Verificar si cache es válido (no expirado)
     */
    public boolean cacheValido(String clave, long tiempoExpiracionMs) {
        JSONObject cacheData = leerJSON("cache_" + clave + ".json");
        try {
            long timestamp = cacheData.getLong("timestamp");
            long tiempoActual = System.currentTimeMillis();
            return (tiempoActual - timestamp) < tiempoExpiracionMs;
        } catch (JSONException e) {
            return false;
        }
    }
    
    /**
     * Limpiar cache expirado
     */
    public void limpiarCacheExpirado(long tiempoExpiracionMs) {
        File directorio = context.getFilesDir();
        File[] archivos = directorio.listFiles();
        if (archivos != null) {
            for (File archivo : archivos) {
                if (archivo.getName().startsWith("cache_") && archivo.getName().endsWith(".json")) {
                    JSONObject cacheData = leerJSON(archivo.getName());
                    try {
                        long timestamp = cacheData.getLong("timestamp");
                        long tiempoActual = System.currentTimeMillis();
                        if ((tiempoActual - timestamp) >= tiempoExpiracionMs) {
                            eliminarArchivo(archivo.getName());
                            Log.d(TAG, "Cache expirado eliminado: " + archivo.getName());
                        }
                    } catch (JSONException e) {
                        // Si no se puede parsear, eliminar
                        eliminarArchivo(archivo.getName());
                    }
                }
            }
        }
    }
    
    // ==================== MÉTODOS DE USUARIO ====================
    
    /**
     * Guardar datos completos de usuario
     */
    public boolean guardarDatosUsuario(JSONObject datosUsuario) {
        return guardarJSON("user_data.json", datosUsuario);
    }
    
    /**
     * Leer datos completos de usuario
     */
    public JSONObject leerDatosUsuario() {
        return leerJSON("user_data.json");
    }
    
    /**
     * Guardar perfil de usuario
     */
    public boolean guardarPerfilUsuario(String nombre, String email, String telefono, String tipo) {
        try {
            JSONObject perfil = new JSONObject();
            perfil.put("nombre", nombre);
            perfil.put("email", email);
            perfil.put("telefono", telefono);
            perfil.put("tipo", tipo);
            perfil.put("fecha_actualizacion", System.currentTimeMillis());
            return guardarDatosUsuario(perfil);
        } catch (JSONException e) {
            Log.e(TAG, "Error creando perfil: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== MÉTODOS DE CONFIGURACIÓN ====================
    
    /**
     * Guardar configuración compleja
     */
    public boolean guardarConfiguracion(String nombreConfig, JSONObject config) {
        return guardarJSON("config_" + nombreConfig + ".json", config);
    }
    
    /**
     * Leer configuración compleja
     */
    public JSONObject leerConfiguracion(String nombreConfig) {
        return leerJSON("config_" + nombreConfig + ".json");
    }
    
    // ==================== MÉTODOS DE BACKUP ====================
    
    /**
     * Crear backup de datos importantes
     */
    public boolean crearBackup(String nombreBackup, JSONObject datos) {
        try {
            JSONObject backup = new JSONObject();
            backup.put("fecha_creacion", System.currentTimeMillis());
            backup.put("version", "1.0");
            backup.put("datos", datos);
            return guardarJSON("backup_" + nombreBackup + ".json", backup);
        } catch (JSONException e) {
            Log.e(TAG, "Error creando backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Restaurar backup
     */
    public JSONObject restaurarBackup(String nombreBackup) {
        JSONObject backup = leerJSON("backup_" + nombreBackup + ".json");
        try {
            return backup.getJSONObject("datos");
        } catch (JSONException e) {
            Log.e(TAG, "Error restaurando backup: " + e.getMessage());
            return new JSONObject();
        }
    }
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Obtener lista de archivos
     */
    public String[] listarArchivos() {
        File directorio = context.getFilesDir();
        File[] archivos = directorio.listFiles();
        if (archivos != null) {
            String[] nombres = new String[archivos.length];
            for (int i = 0; i < archivos.length; i++) {
                nombres[i] = archivos[i].getName();
            }
            return nombres;
        }
        return new String[0];
    }
    
    /**
     * Obtener espacio usado por archivos
     */
    public long obtenerEspacioUsado() {
        File directorio = context.getFilesDir();
        File[] archivos = directorio.listFiles();
        long espacioTotal = 0;
        if (archivos != null) {
            for (File archivo : archivos) {
                espacioTotal += archivo.length();
            }
        }
        return espacioTotal;
    }
    
    /**
     * Limpiar datos de usuario específicos
     */
    public void limpiarDatosUsuario() {
        // Eliminar archivo principal de datos de usuario
        eliminarArchivo(ARCHIVO_DATOS_USUARIO);

        // Eliminar archivos de backup que contengan "registration" o "user"
        File directorio = context.getFilesDir();
        File[] archivos = directorio.listFiles();
        if (archivos != null) {
            for (File archivo : archivos) {
                String nombre = archivo.getName();
                if (nombre.contains("registration") || nombre.contains("user_") || nombre.contains("client_") || nombre.contains("guide_") || nombre.contains("admin_")) {
                    archivo.delete();
                    Log.d(TAG, "Archivo de usuario eliminado: " + nombre);
                }
            }
        }
        Log.d(TAG, "Datos de usuario limpiados");
    }

    /**
     * Limpiar todos los archivos
     */
    public void limpiarTodosLosArchivos() {
        File directorio = context.getFilesDir();
        File[] archivos = directorio.listFiles();
        if (archivos != null) {
            for (File archivo : archivos) {
                archivo.delete();
            }
        }
        Log.d(TAG, "Todos los archivos eliminados");
    }
}

