# Profile Auto-Creation Trigger

## ¿Qué hace este trigger?

Cuando un usuario se registra en tu app (vía email/password o Google Sign-In), automáticamente:

1. ✅ Se crea en `auth.users` (Supabase Auth)
2. ✅ Se crea en `public.profiles` (Trigger automático)

Esto significa que **siempre** tendrás un registro en `profiles` para cada usuario, aunque aún no hayan subido su foto de cuerpo entero.

## Flujo Completo

```sql
Usuario se registra
    ↓
Supabase Auth crea registro en auth.users
    ↓
TRIGGER: on_auth_user_created se dispara
    ↓
FUNCTION: handle_new_user() se ejecuta
    ↓
Se crea registro en profiles con:
  - id: (mismo UUID del usuario)
  - full_body_image_url: NULL (la subirá después)
  - created_at: NOW()
  - updated_at: NOW()
```

## Verificar que el Trigger Funciona

### 1. Ejecutar el Schema Actualizado

Si ya ejecutaste el schema anterior, ejecuta solo esta parte en Supabase SQL Editor:

```sql
-- Hacer que full_body_image_url sea nullable
ALTER TABLE profiles ALTER COLUMN full_body_image_url DROP NOT NULL;

-- Agregar created_at si no existe
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

-- Function: Crear perfil automáticamente
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, full_body_image_url)
    VALUES (NEW.id, NULL);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();
```

### 2. Probar el Trigger

**Opción A: Registrar un nuevo usuario en la app**
1. Abre tu app
2. Registra un nuevo usuario (email/password o Google)
3. Ve a Supabase Dashboard → Table Editor → `profiles`
4. Deberías ver el nuevo registro con `full_body_image_url = NULL`

**Opción B: Verificar en SQL Editor**
```sql
-- Ver todos los perfiles
SELECT * FROM profiles;

-- Verificar que cada usuario tiene perfil
SELECT
    u.id,
    u.email,
    p.id as profile_id,
    p.full_body_image_url,
    p.created_at
FROM auth.users u
LEFT JOIN profiles p ON u.id = p.id;

-- Usuarios SIN perfil (debería estar vacío)
SELECT u.id, u.email
FROM auth.users u
LEFT JOIN profiles p ON u.id = p.id
WHERE p.id IS NULL;
```

## Migrar Usuarios Existentes

Si ya tienes usuarios registrados **antes** de agregar el trigger, necesitas crear sus perfiles manualmente:

```sql
-- Crear perfiles para usuarios existentes que no tienen perfil
INSERT INTO profiles (id, full_body_image_url)
SELECT id, NULL
FROM auth.users
WHERE id NOT IN (SELECT id FROM profiles);
```

## Actualizar el Perfil desde la App

Ahora que el perfil existe automáticamente, puedes actualizarlo desde tu app:

### Kotlin (Update Profile Photo)

```kotlin
// Domain Model
data class ProfileUpdate(
    val fullBodyImageUrl: String
)

// Repository
suspend fun updateProfilePhoto(imageUrl: String): Result<Unit> {
    return try {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("No autenticado"))

        supabase.from("profiles")
            .update({
                set("full_body_image_url", imageUrl)
            }) {
                filter {
                    eq("id", userId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Use Case
class UpdateProfilePhotoUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): Result<Unit> {
        // 1. Upload a Storage
        val imageUrl = repository.uploadProfileImage(imageBytes)
            .getOrElse { return Result.failure(it) }

        // 2. Update en DB
        return repository.updateProfilePhoto(imageUrl)
    }
}
```

### SQL Manual (Testing)

```sql
-- Actualizar foto de perfil manualmente
UPDATE profiles
SET
    full_body_image_url = 'https://...supabase.co/storage/v1/object/public/avatars/...',
    updated_at = NOW()
WHERE id = 'USER_UUID_HERE';
```

## Flujo Completo en la App

### Registro (Automático)
```
Usuario → Register/Login
    ↓
auth.users creado
    ↓
profiles creado (trigger)
    ↓
full_body_image_url = NULL
```

### Subir Foto de Perfil (Manual)
```
Usuario → Profile Screen
    ↓
Selecciona foto
    ↓
Upload a Storage (bucket: avatars)
    ↓
Update profiles.full_body_image_url
    ↓
✅ Listo para Try-On
```

## Verificar RLS Policies

Las políticas de seguridad permiten:

```sql
-- ✅ Usuarios pueden ver su propio perfil
SELECT * FROM profiles WHERE id = auth.uid();

-- ✅ Usuarios pueden actualizar su propio perfil
UPDATE profiles
SET full_body_image_url = 'https://...'
WHERE id = auth.uid();

-- ❌ Usuarios NO pueden ver perfiles de otros
SELECT * FROM profiles WHERE id != auth.uid(); -- Retorna vacío

-- ❌ Usuarios NO pueden actualizar perfiles de otros
UPDATE profiles
SET full_body_image_url = 'https://...'
WHERE id = 'OTHER_USER_ID'; -- Falla por RLS
```

## Troubleshooting

### Error: "new row violates row-level security policy"

**Causa:** El trigger intenta insertar pero la policy RLS lo bloquea.

**Solución:** La función usa `SECURITY DEFINER` que la ejecuta con permisos del creador (superuser), así que debería funcionar. Si falla:

```sql
-- Verificar que la función tiene SECURITY DEFINER
SELECT routine_name, security_type
FROM information_schema.routines
WHERE routine_name = 'handle_new_user';

-- Debería retornar: security_type = 'DEFINER'
```

### Error: "duplicate key value violates unique constraint"

**Causa:** El perfil ya existe para ese usuario.

**Solución:** Normal si el usuario se registró antes del trigger. Usa la query de migración arriba.

### Verificar que el Trigger está Activo

```sql
-- Ver triggers en auth.users
SELECT
    trigger_name,
    event_manipulation,
    action_statement
FROM information_schema.triggers
WHERE event_object_table = 'users'
AND trigger_schema = 'auth';

-- Debería mostrar:
-- trigger_name: on_auth_user_created
-- event_manipulation: INSERT
-- action_statement: EXECUTE FUNCTION public.handle_new_user()
```

## Próximo Paso: Profile Screen

Ahora que el perfil se crea automáticamente, puedes crear:

1. **ProfileScreen.kt** - Para que el usuario suba su foto
2. **ProfileViewModel.kt** - Con `updateProfilePhoto()`
3. **ProfileRepository.kt** - Upload a Storage + Update DB

Ver `MINI.ROADMAP.md` para la implementación del Virtual Try-On que usa esta foto.

---

**Status:** ✅ Perfil se crea automáticamente con cada nuevo registro
