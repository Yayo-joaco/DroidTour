# DroidTour 🏔️

**Sistema de Gestión de Reservas de Tours Locales**

DroidTour es una aplicación móvil nativa para Android que conecta a clientes, guías de turismo, empresas turísticas y superadministradores en una plataforma integral de gestión de tours.

## 📱 Características Principales

### Para Clientes
- ✅ Registro y autenticación
- ✅ Explorar empresas turísticas
- ✅ Realizar reservas con tarjeta
- ✅ QR Check-in/Check-out
- ✅ Seguimiento en tiempo real
- ✅ Chat con empresas
- ✅ Valoraciones y reseñas
- ✅ Historial de tours

### Para Guías de Turismo
- ✅ Registro con aprobación
- ✅ Ver ofertas de tours
- ✅ Aceptar/rechazar trabajos
- ✅ Registrar ubicaciones GPS
- ✅ Escáner QR para check-in/out
- ✅ Seguimiento de tours activos

### Para Administradores de Empresa
- ✅ Registro de empresa completo
- ✅ Crear y gestionar tours
- ✅ Gestión de guías
- ✅ Chat con clientes
- ✅ Reportes de ventas
- ✅ Alertas de check-out

### Para Superadministradores
- ✅ Gestión de usuarios
- ✅ Reportes del sistema
- ✅ Logs de auditoría
- ✅ Aprobación de guías

## 🛠️ Tecnologías

- **Lenguaje**: Java
- **Plataforma**: Android nativo (API 31+)
- **UI**: Material Design 3
- **Build System**: Gradle
- **Base de Datos**: NoSQL (futuro)

## 📋 Requisitos

- Android 12.0 (API Level 31) o superior
- 4 GB RAM mínimo
- 100 MB espacio libre

## 🚀 Instalación

### Prerrequisitos
- Android Studio
- JDK 11+
- Android SDK

### Pasos
1. Clonar el repositorio:
```bash
git clone https://github.com/Yayo-joaco/DroidTour.git
cd DroidTour
```

2. Abrir en Android Studio

3. Sincronizar Gradle:
```bash
./gradlew build
```

4. Ejecutar en dispositivo/emulador

## 🔐 Credenciales de Prueba

Para probar la aplicación:

- **Superadmin**: `superadmin@droidtour.com` / `admin123`
- **Admin Empresa**: `admin@tours.com` / `admin123`
- **Guía**: `guia@tours.com` / `guia123`
- **Cliente**: `cliente@email.com` / `cliente123`

## 📖 Documentación

- [Arquitectura del Sistema](ARCHITECTURE.md)
- [Manual de Instalación](MANUAL_INSTALACION.md)
- [Manual de Usuario](MANUAL_USUARIO.md)
- [Costos OPEX](COSTOS_OPEX.md)

## 🏗️ Estructura del Proyecto

```
app/src/main/
├── java/com/example/droidtour/
│   ├── MainActivity.java (Selector de roles)
│   ├── LoginActivity.java
│   ├── *RegistrationActivity.java (Registros por rol)
│   ├── *MainActivity.java (Dashboards por rol)
│   └── [25+ actividades específicas]
├── res/
│   ├── layout/ (47 layouts XML)
│   ├── drawable/ (Iconos y shapes)
│   ├── values/ (Strings, colores, estilos)
│   └── menu/ (Navigation menus)
└── AndroidManifest.xml
```

## 🎯 Funcionalidades Implementadas

### ✅ Completadas (Mockups)
- Sistema de autenticación completo
- Navegación multi-rol
- Gestión de reservas
- Sistema de chat por empresa
- Seguimiento GPS en tiempo real
- QR Check-in/Check-out
- Sistema de valoraciones
- Reportes y dashboard
- Flujo "Olvidé mi contraseña"

### 🔄 Por Implementar
- Integración con base de datos NoSQL
- API REST backend
- Mapas reales (Google Maps)
- Notificaciones push
- Gateway de pagos real
- Despliegue en la nube

## 💰 Costos Estimados

**OPEX Mensual**: $2,850 - $4,200 USD
- Infraestructura cloud: $290-635
- Servicios externos: $265-1,050
- Desarrollo/mantenimiento: $1,500-2,600
- Licencias: $138
- Seguridad: $290-690

Ver [análisis completo](COSTOS_OPEX.md).

## 🤝 Contribución

1. Fork el proyecto
2. Crear branch (`git checkout -b feature/AmazingFeature`)
3. Commit cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Abrir Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver [LICENSE](LICENSE) para detalles.

## 👥 Equipo

- **Desarrollador Principal**: [Yayo-joaco](https://github.com/Yayo-joaco)

## 📞 Contacto

- **Email**: soporte@droidtour.com
- **GitHub**: [@Yayo-joaco](https://github.com/Yayo-joaco)
- **Proyecto**: [DroidTour](https://github.com/Yayo-joaco/DroidTour)

---

⭐ **¡Dale una estrella si te gusta el proyecto!** ⭐

---

*Desarrollado con ❤️ para la gestión de tours locales*
