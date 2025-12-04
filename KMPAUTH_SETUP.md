# Configuración de KMPAuth - Google Sign-In Nativo

Guía completa para configurar la autenticación nativa de Google usando **KMPAuth** en tu proyecto Kotlin Multiplatform.

## ✅ Implementación Completada

### 1. Dependencias Agregadas

```kotlin
// sharedUI/build.gradle.kts
commonMain.dependencies {
    implementation("io.github.mirzemehdi:kmpauth-google:2.1.0")
    implementation("io.github.mirzemehdi:kmpauth-uihelper:2.1.0")
}
```

### 2. Inicialización en App.kt

```kotlin
@Composable
fun App(...) {
    // Inicializar Google Auth Provider con Web Client ID
    LaunchedEffect(Unit) {
        GoogleAuthProvider.create(
            credentials = GoogleAuthCredentials(
                serverId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            )
        )
    }
    // ...
}
```

### 3. Flujo de Autenticación Implementado

```
Usuario toca "Continuar con Google"
    ↓
GoogleButtonUiContainer maneja el sign-in nativo
    ↓
[ANDROID] Bottom sheet de Google One Tap
[iOS] Google Sign-In SDK
    ↓
Retorna GoogleUser con idToken
    ↓
LoginViewModel.onGoogleSignInResult(idToken)
    ↓
SignInWithGoogleNativeUseCase(idToken)
    ↓
AuthRepository.signInWithGoogleIdToken(idToken)
    ↓
Supabase valida token y crea sesión ✅
```

## 🔧 Configuración Requerida

### Paso 1: Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Navega a **APIs & Services** → **Credentials**
3. Necesitas **DOS** Client IDs:

#### a) Web Client ID (para Supabase)
- Tipo: **OAuth 2.0 Client ID** → **Web application**
- Este ID lo usará Supabase para validar tokens
- Copia el Client ID (termina en `.apps.googleusercontent.com`)

#### b) Android Client ID
- Tipo: **OAuth 2.0 Client ID** → **Android**
- Package name: `baccaro.vestite.app.androidApp`
- Necesitas registrar tu **SHA-1** certificate fingerprint

Para obtener SHA-1:
```bash
# Debug SHA-1
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release SHA-1 (cuando tengas keystore de producción)
keytool -list -v -keystore /path/to/your/keystore.jks -alias your-alias
```

#### c) iOS Client ID (para configuración posterior)
- Tipo: **OAuth 2.0 Client ID** → **iOS**
- Bundle ID: El de tu app iOS (ej: `com.vestite.app`)

### Paso 2: Configurar local.properties

Agrega el **Web Client ID** a `local.properties`:

```properties
# Supabase (ya existente)
supabase.url=https://your-project.supabase.co
supabase.anon.key=your-anon-key

# Google OAuth - Web Client ID
google.web.client.id=TU_WEB_CLIENT_ID.apps.googleusercontent.com
```

⚠️ **IMPORTANTE**: Es el Web Client ID, no el Android Client ID!

### Paso 3: Configurar Supabase Dashboard

1. Ve a **Supabase Dashboard** → **Authentication** → **Providers**
2. Habilita **Google**
3. En **Authorized Client IDs**, agrega:
   - ✅ Tu **Web Client ID**
   - ✅ Tu **Android Client ID**
   - ✅ Tu **iOS Client ID** (cuando lo tengas)
4. Guarda los cambios

## 📱 Configuración por Plataforma

### Android

No se requiere configuración adicional. KMPAuth usa Google Play Services automáticamente.

#### Verificaciones:
- ✅ SHA-1 registrado en Google Cloud Console
- ✅ Android Client ID creado con package name correcto
- ✅ Dispositivo/emulador con Google Play Services

#### Troubleshooting Android:

**Error: "Sign in failed: 10"**
- ✅ Verifica que el SHA-1 esté registrado en el Android Client ID
- ✅ Asegúrate de usar el certificado correcto (debug o release)

**Error: "Sign in failed: 16" (API_NOT_AVAILABLE)**
- ✅ Usa emulador con Google Play (no "Android Open Source")
- ✅ Verifica que Google Play Services esté actualizado

**Bottom sheet no aparece:**
- ✅ Verifica que `google.web.client.id` esté en `local.properties`
- ✅ Check logs de Logcat, busca "KMPAuth" o "GoogleAuth"
- ✅ Rebuild proyecto: `./gradlew clean && ./gradlew :androidApp:assembleDebug`

### iOS

#### Paso 1: Instalar Google Sign-In SDK

En tu proyecto iOS, agrega el SDK usando **Swift Package Manager**:

1. Abre `iosApp/iosApp.xcodeproj` en Xcode
2. **File** → **Add Package Dependencies**
3. URL: `https://github.com/google/GoogleSignIn-iOS`
4. Version: **7.0.0** o superior
5. Add to target: **iosApp**

#### Paso 2: Configurar Info.plist

Agrega estas keys a `iosApp/iosApp/Info.plist`:

```xml
<key>GIDServerClientID</key>
<string>TU_WEB_CLIENT_ID.apps.googleusercontent.com</string>

<key>GIDClientID</key>
<string>TU_IOS_CLIENT_ID.apps.googleusercontent.com</string>

<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <!-- Reversed iOS Client ID (invierte el orden y reemplaza puntos por nada) -->
      <string>com.googleusercontent.apps.TU-IOS-CLIENT-ID-INVERTIDO</string>
    </array>
  </dict>
</array>
```

**Ejemplo de Reversed Client ID:**
- iOS Client ID: `123456-abcdef.apps.googleusercontent.com`
- Reversed: `com.googleusercontent.apps.123456-abcdef`

#### Paso 3: Configurar AppDelegate

Edita tu `iosApp/iOSApp.swift` para manejar el callback de Google:

```swift
import SwiftUI
import shared
import GoogleSignIn

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
        var handled: Bool

        // Manejar Google Sign-In
        handled = GIDSignIn.sharedInstance.handle(url)
        if handled {
            return true
        }

        return false
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL(perform: { url in
                    GIDSignIn.sharedInstance.handle(url)
                })
        }
    }
}
```

## 🧪 Probar la Implementación

### Build Android

```bash
./gradlew clean
./gradlew :androidApp:assembleDebug
```

### Run en Android Studio

1. Sincroniza Gradle
2. Run/Debug → Selecciona `androidApp`
3. Elige emulador o dispositivo
4. Toca "Continuar con Google"
5. Debería aparecer el **bottom sheet nativo** con tus cuentas de Google

### Build iOS

1. Abre `iosApp/iosApp.xcodeproj` en Xcode
2. Selecciona un simulador o dispositivo
3. Run (⌘R)
4. Toca "Continuar con Google"
5. Debería abrir el flujo de Google Sign-In

## 📊 Comparación: Antes vs Después

| Aspecto | OAuth Web (anterior) | KMPAuth (actual) |
|---------|---------------------|------------------|
| **UX Android** | Abre Chrome | Bottom sheet nativo (One Tap) |
| **UX iOS** | Abre Safari | SDK nativo de Google |
| **Tiempo** | ~3-5 segundos | ~1 segundo |
| **Pasos usuario** | 3-4 clics + navegación | 1 clic |
| **Experiencia** | Sale de la app | Dentro de la app |
| **Setup complejidad** | Media | Baja (KMPAuth lo maneja) |
| **Mantenimiento** | Manual | Manejado por librería |

## 🔍 Debugging

### Logs útiles

**Android:**
```bash
adb logcat | grep -E "KMPAuth|GoogleAuth|Supabase"
```

**iOS (en Xcode):**
Busca en Console:
- "KMPAuth"
- "GIDSignIn"
- "Supabase"

### Verificar configuración

1. **¿BuildConfig tiene el Web Client ID?**
   ```kotlin
   println("Web Client ID: ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")
   ```

2. **¿GoogleAuthProvider está inicializado?**
   Debería inicializarse en `App.kt` con `LaunchedEffect`

3. **¿Supabase recibe el token?**
   Check logs en `AuthRepositoryImpl.signInWithGoogleIdToken()`

## 📚 Recursos

- **KMPAuth GitHub**: https://github.com/mirzemehdi/KMPAuth
- **KMPAuth Docs**: https://kmpauth.com/
- **Google Cloud Console**: https://console.cloud.google.com/
- **Supabase Auth Docs**: https://supabase.com/docs/guides/auth/social-login/auth-google
- **Google Sign-In iOS**: https://developers.google.com/identity/sign-in/ios

## 🎯 Próximos Pasos

- ✅ Configurar Web Client ID en `local.properties`
- ✅ Registrar SHA-1 en Google Cloud Console (Android)
- ✅ Agregar Client IDs a Supabase Dashboard
- ✅ Probar en Android
- ⬜ Configurar iOS (Info.plist + AppDelegate)
- ⬜ Probar en iOS

## ❓ Preguntas Frecuentes

**P: ¿Por qué necesito el Web Client ID si tengo el de Android?**
R: El Web Client ID es el que Supabase usa para validar el token. El Android Client ID solo se usa para identificar tu app en Google.

**P: ¿Funciona en emuladores?**
R: Sí, pero el emulador Android debe tener Google Play Services. Usa un emulador con Play Store.

**P: ¿Necesito deep links para esto?**
R: No. KMPAuth usa el SDK nativo, no requiere deep links como el flujo OAuth web tradicional.

**P: ¿Puedo usar el botón de UI de KMPAuth?**
R: Sí! Puedes usar `GoogleSignInButton` de `kmpauth-uihelper`:
```kotlin
GoogleButtonUiContainer(onGoogleSignInResult = { ... }) {
    GoogleSignInButton(modifier = Modifier.fillMaxWidth()) {
        this.onClick()
    }
}
```

**P: ¿Qué pasa con el flujo OAuth web anterior?**
R: Sigue disponible en `SignInWithGoogleUseCase` por si lo necesitas de fallback o para web.
