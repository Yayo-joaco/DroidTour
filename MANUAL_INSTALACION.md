# Manual de Instalación - DroidTour

## Requisitos del Sistema

### Requisitos Mínimos
- **Sistema Operativo**: Windows 10/11, macOS 10.14+, o Linux Ubuntu 18.04+
- **RAM**: 8 GB mínimo, 16 GB recomendado
- **Almacenamiento**: 10 GB de espacio libre
- **Java**: JDK 11 o superior
- **Android Studio**: Versión más reciente

### Dispositivo Android
- **Versión Mínima**: Android 12.0 (API Level 31)
- **RAM**: 4 GB mínimo
- **Almacenamiento**: 100 MB para la aplicación

## Instalación del Entorno de Desarrollo

### 1. Instalar Android Studio

1. Descargar Android Studio desde: https://developer.android.com/studio
2. Ejecutar el instalador y seguir las instrucciones
3. Configurar el SDK de Android:
   - Abrir SDK Manager
   - Instalar Android 12.0 (API 31) y superiores
   - Instalar Android SDK Build-Tools
   - Instalar Google Play Services

### 2. Configurar Variables de Entorno

#### Windows
```bash
# Agregar al PATH del sistema
ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

#### macOS/Linux
```bash
# Agregar al ~/.bashrc o ~/.zshrc
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### 3. Instalar Git
- Windows: https://git-scm.com/download/win
- macOS: `brew install git`
- Linux: `sudo apt install git`

## Instalación del Proyecto

### 1. Clonar el Repositorio
```bash
git clone https://github.com/tu-usuario/droidtour.git
cd droidtour
```

### 2. Abrir en Android Studio
1. Abrir Android Studio
2. Seleccionar "Open an existing project"
3. Navegar a la carpeta del proyecto clonado
4. Esperar a que Gradle sincronice las dependencias

### 3. Configurar el Proyecto
```bash
# Limpiar y construir el proyecto
./gradlew clean
./gradlew build
```

## Configuración de Dispositivo

### Opción 1: Dispositivo Físico
1. Habilitar "Opciones de desarrollador":
   - Ir a Configuración > Acerca del teléfono
   - Tocar "Número de compilación" 7 veces
2. Habilitar "Depuración USB"
3. Conectar el dispositivo via USB
4. Autorizar la conexión de depuración

### Opción 2: Emulador
1. Abrir AVD Manager en Android Studio
2. Crear un nuevo dispositivo virtual:
   - Seleccionar un dispositivo (ej: Pixel 6)
   - Elegir Android 12.0 (API 31) o superior
   - Configurar RAM: 4 GB mínimo
3. Iniciar el emulador

## Ejecución de la Aplicación

### 1. Desde Android Studio
1. Seleccionar el dispositivo/emulador en la barra superior
2. Hacer clic en el botón "Run" (▶️)
3. Esperar a que la aplicación se instale y ejecute

### 2. Desde Línea de Comandos
```bash
# Compilar y instalar en dispositivo conectado
./gradlew installDebug

# Solo compilar APK
./gradlew assembleDebug
# El APK se genera en: app/build/outputs/apk/debug/
```

## Verificación de la Instalación

### 1. Pruebas Básicas
1. **Login**: Probar con credenciales mock:
   - Superadmin: `superadmin@droidtour.com` / `admin123`
   - Admin: `admin@tours.com` / `admin123`
   - Guía: `guia@tours.com` / `guia123`
   - Cliente: `cliente@email.com` / `cliente123`

2. **Navegación**: Verificar que todas las pantallas cargan correctamente

3. **Funcionalidades**: Probar registro, reservas, chat, etc.

### 2. Logs de Depuración
```bash
# Ver logs en tiempo real
adb logcat | grep DroidTour

# Ver logs específicos de la app
adb logcat -s "YourAppTag"
```

## Solución de Problemas Comunes

### Error: "SDK not found"
```bash
# Verificar ANDROID_HOME
echo $ANDROID_HOME  # Linux/macOS
echo %ANDROID_HOME% # Windows

# Reinstalar SDK si es necesario
```

### Error: "Gradle sync failed"
1. File > Invalidate Caches and Restart
2. Verificar conexión a internet
3. Actualizar Gradle en `gradle/wrapper/gradle-wrapper.properties`

### Error: "Device not found"
```bash
# Verificar dispositivos conectados
adb devices

# Reiniciar ADB si es necesario
adb kill-server
adb start-server
```

### Error: "Build failed"
```bash
# Limpiar proyecto
./gradlew clean

# Verificar versión de Java
java -version
# Debe ser Java 11 o superior
```

## Configuración para Producción

### 1. Firmar la APK
1. Generar keystore:
```bash
keytool -genkey -v -keystore droidtour-key.keystore -alias droidtour -keyalg RSA -keysize 2048 -validity 10000
```

2. Configurar en `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            keyAlias 'droidtour'
            keyPassword 'your_password'
            storeFile file('droidtour-key.keystore')
            storePassword 'your_password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

3. Generar APK de release:
```bash
./gradlew assembleRelease
```

### 2. Configurar Backend (Futuro)
- Configurar base de datos NoSQL
- Configurar servidor de API
- Configurar servicios de push notifications
- Configurar gateway de pagos

## Mantenimiento

### Actualizaciones
```bash
# Actualizar dependencias
./gradlew dependencyUpdates

# Actualizar Gradle wrapper
./gradlew wrapper --gradle-version=latest
```

### Backup
- Respaldar keystore de firma
- Respaldar configuraciones de proyecto
- Versionar cambios con Git

---

## Contacto y Soporte

Para problemas técnicos o consultas:
- **Email**: soporte@droidtour.com
- **Documentación**: Ver `ARCHITECTURE.md`
- **Issues**: GitHub Issues del repositorio

---

*Última actualización: Diciembre 2024*
*Versión: 1.0.0*
