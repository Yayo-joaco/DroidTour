package com.example.droidtour.managers;

import android.content.Context;
import android.widget.Toast;
import com.example.droidtour.utils.PreferencesManager;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * LocalStorageExample - Ejemplos de uso de PreferencesManager y FileManager
 */
public class LocalStorageExample {
    
    private Context context;
    private PreferencesManager prefsManager;
    private FileManager fileManager;
    
    public LocalStorageExample(Context context) {
        this.context = context;
        this.prefsManager = new PreferencesManager(context);
        this.fileManager = new FileManager(context);
    }
    
    // ==================== EJEMPLOS DE SHAREDPREFERENCES ====================
    
    /**
     * Ejemplo 1: Login de usuario
     */
    public void ejemploLogin() {
        // Al hacer login exitoso
        prefsManager.guardarUsuario("user123", "Ana García", "ana@email.com", "CLIENT");
        prefsManager.guardarAccessToken("token_abc123");
        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
        
        Toast.makeText(context, "Usuario logueado: " + prefsManager.obtenerUsuario(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Ejemplo 2: Configuraciones de la app
     */
    public void ejemploConfiguraciones() {
        // Guardar preferencias del usuario
        prefsManager.guardarTemaOscuro(true);
        prefsManager.guardarNotificaciones(false);
        prefsManager.guardarSonidos(true);
        prefsManager.guardarIdioma("es");
        
        // Leer configuraciones
        boolean temaOscuro = prefsManager.esTemaOscuro();
        boolean notificaciones = prefsManager.notificacionesActivas();
        String idioma = prefsManager.obtenerIdioma();
        
        Toast.makeText(context, 
            "Tema oscuro: " + temaOscuro + 
            ", Notificaciones: " + notificaciones + 
            ", Idioma: " + idioma, 
            Toast.LENGTH_LONG).show();
    }
    
    /**
     * Ejemplo 3: Verificar sesión
     */
    public void ejemploVerificarSesion() {
        if (prefsManager.sesionActiva()) {
            String usuario = prefsManager.obtenerUsuario();
            String tipo = prefsManager.obtenerTipoUsuario();
            Toast.makeText(context, "Sesión activa: " + usuario + " (" + tipo + ")", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "No hay sesión activa", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Ejemplo 4: Logout
     */
    public void ejemploLogout() {
        prefsManager.cerrarSesion();
        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show();
    }
    
    // ==================== EJEMPLOS DE FILEMANAGER ====================
    
    /**
     * Ejemplo 1: Guardar datos de usuario completos
     */
    public void ejemploGuardarDatosUsuario() {
        try {
            JSONObject datosUsuario = new JSONObject();
            datosUsuario.put("nombre", "Carlos López");
            datosUsuario.put("email", "carlos@email.com");
            datosUsuario.put("telefono", "999888777");
            datosUsuario.put("direccion", "Av. Principal 123");
            datosUsuario.put("fecha_nacimiento", "1990-05-15");
            datosUsuario.put("preferencias", new JSONObject()
                .put("tema", "oscuro")
                .put("notificaciones", true)
                .put("idioma", "es")
            );
            datosUsuario.put("fecha_registro", System.currentTimeMillis());
            
            boolean guardado = fileManager.guardarDatosUsuario(datosUsuario);
            if (guardado) {
                Toast.makeText(context, "Datos de usuario guardados", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(context, "Error guardando datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Ejemplo 2: Leer datos de usuario
     */
    public void ejemploLeerDatosUsuario() {
        JSONObject datosUsuario = fileManager.leerDatosUsuario();
        try {
            String nombre = datosUsuario.getString("nombre");
            String email = datosUsuario.getString("email");
            Toast.makeText(context, "Usuario: " + nombre + " (" + email + ")", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(context, "Error leyendo datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Ejemplo 3: Cache de API
     */
    public void ejemploCacheAPI() {
        // Simular respuesta de API
        String respuestaAPI = "{\"tours\": [{\"id\": 1, \"nombre\": \"Machu Picchu\", \"precio\": 450}, {\"id\": 2, \"nombre\": \"Lima Centro\", \"precio\": 120}]}";
        
        // Guardar en cache (válido por 1 hora)
        fileManager.guardarCache("tours_populares", respuestaAPI);
        
        // Verificar si cache es válido
        boolean cacheValido = fileManager.cacheValido("tours_populares", 3600000); // 1 hora en ms
        
        if (cacheValido) {
            String cache = fileManager.leerCache("tours_populares");
            Toast.makeText(context, "Cache válido: " + cache.substring(0, 50) + "...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Cache expirado", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Ejemplo 4: Backup de datos
     */
    public void ejemploBackup() {
        try {
            JSONObject datosImportantes = new JSONObject();
            datosImportantes.put("reservas", new JSONObject()
                .put("total", 5)
                .put("activas", 2)
                .put("completadas", 3)
            );
            datosImportantes.put("configuracion", new JSONObject()
                .put("tema", "oscuro")
                .put("notificaciones", true)
            );
            
            boolean backupCreado = fileManager.crearBackup("datos_importantes", datosImportantes);
            if (backupCreado) {
                Toast.makeText(context, "Backup creado exitosamente", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(context, "Error creando backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Ejemplo 5: Restaurar backup
     */
    public void ejemploRestaurarBackup() {
        JSONObject backup = fileManager.restaurarBackup("datos_importantes");
        try {
            JSONObject reservas = backup.getJSONObject("reservas");
            int total = reservas.getInt("total");
            Toast.makeText(context, "Backup restaurado - Total reservas: " + total, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(context, "Error restaurando backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // ==================== EJEMPLOS COMBINADOS ====================
    
    /**
     * Ejemplo completo: Flujo de login con datos persistentes
     */
    public void ejemploFlujoCompleto() {
        // 1. Verificar si es primera vez
        if (prefsManager.esPrimeraVez()) {
            Toast.makeText(context, "¡Bienvenido por primera vez!", Toast.LENGTH_SHORT).show();
            prefsManager.marcarPrimeraVezCompletada();
        }
        
        // 2. Verificar sesión existente
        if (prefsManager.sesionActiva()) {
            String usuario = prefsManager.obtenerUsuario();
            Toast.makeText(context, "Bienvenido de vuelta, " + usuario, Toast.LENGTH_SHORT).show();
        } else {
            // 3. Simular login
            prefsManager.guardarUsuario("user456", "María Rodríguez", "maria@email.com", "GUIDE");
            
            // 4. Guardar datos completos en archivo
            try {
                JSONObject perfilCompleto = new JSONObject();
                perfilCompleto.put("nombre", "María Rodríguez");
                perfilCompleto.put("email", "maria@email.com");
                perfilCompleto.put("tipo", "GUIDE");
                perfilCompleto.put("especialidades", new JSONObject()
                    .put("tours_históricos", true)
                    .put("tours_naturales", false)
                    .put("tours_aventura", true)
                );
                perfilCompleto.put("calificacion", 4.8);
                perfilCompleto.put("tours_completados", 25);
                
                fileManager.guardarDatosUsuario(perfilCompleto);
                
                // 5. Crear backup
                fileManager.crearBackup("perfil_usuario", perfilCompleto);
                
                Toast.makeText(context, "Login exitoso y datos guardados", Toast.LENGTH_SHORT).show();
                
            } catch (JSONException e) {
                Toast.makeText(context, "Error en el flujo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Limpiar todo el local storage (para testing)
     */
    public void limpiarTodo() {
        prefsManager.limpiarTodo();
        fileManager.limpiarTodosLosArchivos();
        Toast.makeText(context, "Local storage completamente limpiado", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Mostrar información del local storage
     */
    public void mostrarInfoLocalStorage() {
        // Info de SharedPreferences
        String usuario = prefsManager.obtenerUsuario();
        boolean sesionActiva = prefsManager.sesionActiva();
        boolean primeraVez = prefsManager.esPrimeraVez();

        // Info de archivos
        String[] archivos = fileManager.listarArchivos();
        long espacioUsado = fileManager.obtenerEspacioUsado();

        String info = String.format(
            "Usuario: %s\nSesión: %s\nPrimera vez: %s\nArchivos: %d\nEspacio: %d bytes",
            usuario, sesionActiva, primeraVez, archivos.length, espacioUsado
        );

        Toast.makeText(context, info, Toast.LENGTH_LONG).show();
    }

    // ==================== EJEMPLOS DE REGISTRO ====================

    /**
     * Ejemplo de registro de cliente con Local Storage
     */
    public void ejemploRegistroCliente() {
        try {
            // Simular datos de registro de cliente
            String clientId = "CLIENT_" + System.currentTimeMillis();
            String fullName = "Ana García Pérez";
            String email = "ana.garcia@email.com";

            // 1. Guardar en SharedPreferences
            prefsManager.guardarUsuario(clientId, fullName, email, "CLIENT");
            prefsManager.guardarAccessToken("token_" + System.currentTimeMillis());

            // 2. Guardar datos completos en archivo
            JSONObject clientData = new JSONObject();
            clientData.put("id", clientId);
            clientData.put("firstName", "Ana");
            clientData.put("lastName", "García Pérez");
            clientData.put("fullName", fullName);
            clientData.put("email", email);
            clientData.put("phone", "987654321");
            clientData.put("address", "Av. Principal 123");
            clientData.put("userType", "CLIENT");
            clientData.put("registrationDate", System.currentTimeMillis());
            clientData.put("status", "ACTIVE");

            boolean saved = fileManager.guardarDatosUsuario(clientData);

            if (saved) {
                fileManager.crearBackup("client_registration_" + clientId, clientData);
                Toast.makeText(context, "Cliente registrado: " + fullName, Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            Toast.makeText(context, "Error en registro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Ejemplo de registro de guía con Local Storage
     */
    public void ejemploRegistroGuia() {
        try {
            // Simular datos de registro de guía
            String guideId = "GUIDE_" + System.currentTimeMillis();
            String fullName = "Carlos Mendoza";
            String email = "carlos.mendoza@email.com";

            // 1. Guardar en SharedPreferences
            prefsManager.guardarUsuario(guideId, fullName, email, "GUIDE");
            prefsManager.guardarAccessToken("token_" + System.currentTimeMillis());

            // 2. Guardar datos completos en archivo
            JSONObject guideData = new JSONObject();
            guideData.put("id", guideId);
            guideData.put("firstName", "Carlos");
            guideData.put("lastName", "Mendoza");
            guideData.put("fullName", fullName);
            guideData.put("email", email);
            guideData.put("phone", "912345678");
            guideData.put("userType", "GUIDE");
            guideData.put("registrationDate", System.currentTimeMillis());
            guideData.put("status", "PENDING_APPROVAL");
            guideData.put("approved", false);

            // Datos específicos del guía
            JSONObject guideInfo = new JSONObject();
            guideInfo.put("experience", "3 años en turismo cultural");
            guideInfo.put("languages", "Español, Inglés, Quechua");
            guideInfo.put("specialties", "Historia, Arqueología");
            guideData.put("guide_info", guideInfo);

            boolean saved = fileManager.guardarDatosUsuario(guideData);

            if (saved) {
                fileManager.crearBackup("guide_registration_" + guideId, guideData);
                Toast.makeText(context, "Guía registrado: " + fullName + " (Pendiente aprobación)", Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {
            Toast.makeText(context, "Error en registro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Ejemplo de registro de admin con empresa
     */
    public void ejemploRegistroAdmin() {
        try {
            // Simular datos de registro de admin
            String adminId = "ADMIN_" + System.currentTimeMillis();
            String companyId = "COMPANY_" + System.currentTimeMillis();
            String fullName = "María Rodríguez";
            String email = "maria.rodriguez@email.com";
            String companyName = "Tours Machu Picchu SAC";

            // 1. Guardar admin en SharedPreferences
            prefsManager.guardarUsuario(adminId, fullName, email, "ADMIN");
            prefsManager.guardarAccessToken("token_" + System.currentTimeMillis());
            prefsManager.guardar("company_id", companyId);
            prefsManager.guardar("company_name", companyName);

            // 2. Guardar datos del admin
            JSONObject adminData = new JSONObject();
            adminData.put("id", adminId);
            adminData.put("firstName", "María");
            adminData.put("lastName", "Rodríguez");
            adminData.put("fullName", fullName);
            adminData.put("email", email);
            adminData.put("phone", "998877665");
            adminData.put("userType", "ADMIN");
            adminData.put("companyId", companyId);
            adminData.put("registrationDate", System.currentTimeMillis());
            adminData.put("status", "PENDING_APPROVAL");

            // 3. Guardar datos de la empresa
            JSONObject companyData = new JSONObject();
            companyData.put("id", companyId);
            companyData.put("name", companyName);
            companyData.put("email", "info@toursmp.com");
            companyData.put("phone", "084-123456");
            companyData.put("address", "Av. Sol 123, Cusco");
            companyData.put("adminId", adminId);
            companyData.put("registrationDate", System.currentTimeMillis());
            companyData.put("status", "PENDING_APPROVAL");

            boolean adminSaved = fileManager.guardarDatosUsuario(adminData);
            boolean companySaved = fileManager.guardarJSON("company_" + companyId + ".json", companyData);

            if (adminSaved && companySaved) {
                fileManager.crearBackup("admin_registration_" + adminId, adminData);
                fileManager.crearBackup("company_registration_" + companyId, companyData);
                Toast.makeText(context, "Admin y empresa registrados:\n" + fullName + "\n" + companyName, Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {
            Toast.makeText(context, "Error en registro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Ejemplo de verificación de datos registrados
     */
    public void ejemploVerificarRegistros() {
        // Verificar datos en SharedPreferences
        String usuario = prefsManager.obtenerUsuario();
        String tipoUsuario = prefsManager.obtenerTipoUsuario();
        boolean sesionActiva = prefsManager.sesionActiva();

        // Verificar datos en archivos
        JSONObject userData = fileManager.leerDatosUsuario();
        String[] archivos = fileManager.listarArchivos();

        String info = String.format(
            "DATOS REGISTRADOS:\n" +
            "Usuario: %s\n" +
            "Tipo: %s\n" +
            "Sesión: %s\n" +
            "Archivos: %d\n" +
            "Datos completos: %s",
            usuario, tipoUsuario, sesionActiva, archivos.length,
            userData.toString().equals("{}") ? "No" : "Sí"
        );

        Toast.makeText(context, info, Toast.LENGTH_LONG).show();
    }
}


