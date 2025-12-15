# Configuración de Reglas de Seguridad - Firebase Realtime Database

## ⚠️ PROBLEMA ACTUAL
Estás recibiendo errores de "Permission denied" porque las reglas de seguridad están bloqueando el acceso.

## 📋 PASOS PARA CONFIGURAR LAS REGLAS

### Paso 1: Acceder a Firebase Console
1. Ve a: https://console.firebase.google.com/
2. Selecciona tu proyecto: **droidtour**
3. En el menú lateral, haz clic en **"Realtime Database"**

### Paso 2: Ir a la pestaña "Rules"
1. En la parte superior de la página de Realtime Database, haz clic en la pestaña **"Rules"**

### Paso 3: Copiar las Reglas
Copia el contenido del archivo `database-rules.json` que está en la raíz del proyecto.

### Paso 4: Pegar y Publicar
1. Reemplaza el contenido actual de las reglas con el contenido de `database-rules.json`
2. Haz clic en **"Publish"** (Publicar)

## 🔒 REGLAS DE SEGURIDAD EXPLICADAS

### Conversaciones (`/conversations`)
- **Lectura**: Solo usuarios autenticados que sean el cliente o la empresa de la conversación
- **Escritura**: Solo el cliente o administradores de la empresa pueden escribir

### Mensajes (`/conversations/{id}/messages`)
- **Lectura**: Mismos permisos que la conversación padre
- **Escritura**: 
  - El remitente puede crear mensajes
  - Cualquiera puede actualizar el estado (DELIVERED, READ)

### Presencia de Usuario (`/user_presence`)
- **Lectura**: Cualquier usuario autenticado puede leer presencia
- **Escritura**: Solo puedes escribir tu propia presencia

### Índice de Conversaciones (`/conversation_index`)
- **Lectura/Escritura**: Solo puedes acceder a tus propios índices

## 🧪 REGLAS TEMPORALES PARA PRUEBAS (NO USAR EN PRODUCCIÓN)

Si necesitas probar rápidamente sin autenticación, puedes usar estas reglas temporales:

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

⚠️ **ADVERTENCIA**: Estas reglas permiten que cualquier usuario autenticado lea/escriba todo. Solo úsalas para desarrollo.

## ✅ VERIFICACIÓN

Después de publicar las reglas:
1. Reinicia la app
2. Los errores de "Permission denied" deberían desaparecer
3. Deberías poder crear conversaciones y enviar mensajes

## 📝 NOTAS ADICIONALES

- Las reglas se aplican inmediatamente después de publicar
- Si cambias las reglas, puede tomar unos segundos en propagarse
- Siempre prueba las reglas antes de usar en producción

