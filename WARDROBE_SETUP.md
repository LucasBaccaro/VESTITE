# Wardrobe Feature Setup Guide

Esta guía te ayudará a configurar la feature de Guardarropa (Wardrobe) que incluye:
- Análisis de prendas con Gemini AI
- Almacenamiento de imágenes en Supabase Storage
- Gestión de guardarropa personal

## 1. Configuración de Supabase

### 1.1 Ejecutar el Schema SQL

1. Ve a tu proyecto en [Supabase Dashboard](https://app.supabase.com)
2. Navega a **SQL Editor**
3. Copia el contenido completo de `supabase/schema.sql`
4. Pega y ejecuta el SQL

Esto creará:
- Tabla `profiles` (fotos de perfil)
- Tabla `categories` (categorías de prendas: upper, lower, footwear)
- Tabla `garments` (prendas del usuario)
- Tabla `outfits` (outfits generados)
- Políticas de seguridad RLS (Row Level Security)

### 1.2 Configurar Storage Buckets

1. Ve a **Storage** en tu Supabase Dashboard
2. Crea los siguientes buckets:

**Bucket: `garments`**
- Visibilidad: **Public** (para poder mostrar las imágenes en la app)
- File size limit: 10 MB
- Allowed MIME types: `image/jpeg`, `image/png`, `image/webp`

**Bucket: `avatars`** (opcional, para futuro)
- Visibilidad: **Public**
- File size limit: 5 MB
- Allowed MIME types: `image/jpeg`, `image/png`

**Bucket: `outfits`** (opcional, para futuro)
- Visibilidad: **Private**
- File size limit: 10 MB
- Allowed MIME types: `image/jpeg`, `image/png`

### 1.3 Configurar Storage Policies

Para el bucket `garments`, asegúrate de tener estas políticas:

```sql
-- Policy: Users can upload their own garments
CREATE POLICY "Users can upload garments"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'garments' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- Policy: Users can view their own garments
CREATE POLICY "Users can view own garments"
ON storage.objects FOR SELECT
TO authenticated
USING (
  bucket_id = 'garments' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- Policy: Users can delete their own garments
CREATE POLICY "Users can delete own garments"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'garments' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- Policy: Allow public read access (for displaying images)
CREATE POLICY "Public garments are viewable"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'garments');
```

## 2. Configuración de Gemini AI

### 2.1 Obtener API Key de Gemini

1. Ve a [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Inicia sesión con tu cuenta de Google
3. Click en **Get API Key**
4. Copia tu API Key

### 2.2 Agregar API Key al proyecto

Abre el archivo `local.properties` en la raíz del proyecto y agrega:

```properties
# Existing keys
supabase.url=https://your-project.supabase.co
supabase.anon.key=your-anon-key
google.web.client.id=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com

# Add Gemini API Key
gemini.api.key=YOUR_GEMINI_API_KEY
```

**Importante:** El archivo `local.properties` está en `.gitignore` y nunca se subirá a Git.

## 3. Arquitectura de la Feature

### Flujo de Carga de Prendas

```
Usuario selecciona imagen
    ↓
1. Imagen → Gemini Flash (Análisis)
    ↓
   Retorna: {description, fit}
    ↓
2. Imagen → Supabase Storage (Upload)
    ↓
   Retorna: URL pública
    ↓
3. Guardar en DB (garments table)
   - image_url
   - ai_description
   - ai_fit
   - category_id
```

### Modelos de Gemini Utilizados

- **Gemini 2.0 Flash** (`gemini-2.0-flash-exp`): Para análisis rápido de prendas
  - Input: Imagen de la prenda
  - Output: JSON con `description` y `fit`
  - Costo: Muy bajo
  - Velocidad: Rápida (~1-2 segundos)

## 4. Estructura del Código

### Feature Module Structure

```
features/wardrobe/
├── data/
│   ├── remote/
│   │   ├── dto/          # DTOs de Gemini y Supabase
│   │   └── repository/   # GeminiRepository, GarmentRepositoryImpl
│   └── mapper/           # Mappers DTO → Domain
├── domain/
│   ├── model/            # Category, Garment, GarmentMetadata
│   ├── repository/       # GarmentRepository (interface)
│   └── usecase/          # Upload, Get, Delete use cases
├── presentation/
│   ├── list/             # WardrobeListScreen + ViewModel
│   └── upload/           # UploadGarmentScreen + ViewModel
└── di/
    └── WardrobeModule.kt # Koin DI
```

### Navegación

- `Home` → **Ver mi Guardarropa** → `WardrobeList`
- `WardrobeList` → **+** → `UploadGarment`
- `UploadGarment` → Seleccionar imagen → Análisis automático → Guardar

## 5. Próximos Pasos (TODO)

### Implementación de Image Picker

La pantalla `UploadGarmentScreen` tiene un placeholder para seleccionar imágenes:

```kotlin
onImagePicker = { onImageSelected ->
    // TODO: Implement platform-specific image picker
    onImageSelected(null, null)
}
```

**Android:** Usar `ActivityResultContracts.GetContent()`
**iOS:** Usar `UIImagePickerController` o `PHPickerViewController`

### Implementación de Profile Photo

Los usuarios necesitan subir una foto de cuerpo entero para el Virtual Try-On:
- Pantalla: `ProfileScreen`
- Tabla: `profiles`
- Bucket: `avatars`

### Try-On con Gemini 3 Pro (Siguiente Fase)

Ver `MINI.ROADMAP.md` para la implementación del Virtual Try-On que combina:
- Foto del usuario
- 1-3 prendas seleccionadas
- Gemini 3 Pro Image Preview
- Prompt de preservación de identidad

## 6. Testing

### Verificar que todo funciona:

1. **Build del proyecto:**
   ```bash
   ./gradlew :sharedUI:build
   ```

2. **Ejecutar la app:**
   ```bash
   ./gradlew :androidApp:assembleDebug
   ```

3. **Flujo de prueba:**
   - Login con tu cuenta
   - Click en "Ver mi Guardarropa"
   - Click en "+" para agregar prenda
   - Seleccionar categoría (Superior, Inferior, Calzado)
   - Seleccionar imagen (cuando implementes el picker)
   - Verificar que la IA analiza la prenda
   - Verificar que la prenda aparece en la lista

## 7. Troubleshooting

### Error: "placeholder-gemini-key"

- Verifica que agregaste `gemini.api.key` en `local.properties`
- Rebuild el proyecto: `./gradlew clean build`

### Error: "Bucket not found"

- Verifica que creaste el bucket `garments` en Supabase Storage
- Verifica que el bucket es **public**

### Error: "Row Level Security policy violation"

- Verifica que ejecutaste el schema SQL completo
- Verifica que las políticas RLS están activas
- Verifica que el usuario está autenticado

### Imágenes no se cargan

- Verifica que el bucket `garments` es **public**
- Verifica la URL en la base de datos
- Verifica las políticas de Storage

## 8. Recursos

- [Supabase Storage Docs](https://supabase.com/docs/guides/storage)
- [Gemini API Docs](https://ai.google.dev/docs)
- [KMPAuth Docs](https://github.com/mirzemehdi/KMPAuth)
- [Supabase-KT Docs](https://supabase.com/docs/reference/kotlin/introduction)

---

**¡La feature está lista para usar!** Solo necesitas implementar el Image Picker específico de plataforma para completar el flujo de carga de prendas.
