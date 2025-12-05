Funcionalidades actuales (JavaScript/React Native):

1. Virtual Try-On Service:
- Usa Google Gemini AI (generativelanguage.googleapis.com)
- Genera imágenes fotorrealistas de personas vistiendo prendas
- Soporta 1-3 prendas simultáneas (upper/lower/footwear)
- Dos modelos disponibles:
    - gemini-3-pro-image-preview (alta calidad, aspecto 3:4, resolución 2K)
    - gemini-2.5-flash-image (más rápido y estable)
- Input: imagen usuario + imágenes de prendas + descripciones
- Output: imagen base64
2. Supabase Client:
- Configuración básica (ya implementado en KMP)

Funcionalidades que FALTAN en SERVICES.md pero son necesarias:

- Sistema de gestión de prendas (CRUD)
- Almacenamiento de imágenes en Supabase Storage
- Base de datos relacional para prendas, categorías, outfits
- Historial de try-ons generados
- Integración con usuario autenticado

 ---
Roadmap de Implementación

FASE 1: Database Schema (Supabase)

Objetivo: Crear la estructura de base de datos en Supabase para almacenar prendas, categorías, y try-ons generados.

1.1 Tablas de Supabase

Tabla: categories
CREATE TABLE categories (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
name TEXT NOT NULL UNIQUE, -- 'upper', 'lower', 'footwear', 'accessories'
display_name TEXT NOT NULL, -- 'Parte superior', 'Parte inferior', etc.
icon TEXT, -- URL o nombre del icono
created_at TIMESTAMPTZ DEFAULT NOW()
);

Tabla: garments (prendas)
CREATE TABLE garments (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
category_id UUID NOT NULL REFERENCES categories(id),

     -- Información de la prenda
     name TEXT NOT NULL,
     description TEXT,
     type TEXT NOT NULL, -- 't-shirt', 'jeans', 'sneakers', etc.
     color TEXT,
     brand TEXT,
     fit TEXT NOT NULL DEFAULT 'regular', -- 'tight', 'regular', 'loose'

     -- Imágenes
     image_url TEXT NOT NULL, -- URL en Supabase Storage
     thumbnail_url TEXT, -- Miniatura

     -- Metadata
     is_favorite BOOLEAN DEFAULT false,
     wear_count INTEGER DEFAULT 0, -- Cuántas veces se usó en try-ons

     created_at TIMESTAMPTZ DEFAULT NOW(),
     updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_garments_user_id ON garments(user_id);
CREATE INDEX idx_garments_category_id ON garments(category_id);
CREATE INDEX idx_garments_is_favorite ON garments(is_favorite);

Tabla: outfits (combinaciones guardadas)
CREATE TABLE outfits (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

     name TEXT NOT NULL,
     description TEXT,

     upper_garment_id UUID REFERENCES garments(id) ON DELETE SET NULL,
     lower_garment_id UUID REFERENCES garments(id) ON DELETE SET NULL,
     footwear_id UUID REFERENCES garments(id) ON DELETE SET NULL,

     is_favorite BOOLEAN DEFAULT false,

     created_at TIMESTAMPTZ DEFAULT NOW(),
     updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_outfits_user_id ON outfits(user_id);

Tabla: tryon_results (resultados de try-ons generados)
CREATE TABLE tryon_results (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
outfit_id UUID REFERENCES outfits(id) ON DELETE SET NULL,

     -- Referencias a prendas usadas
     upper_garment_id UUID REFERENCES garments(id) ON DELETE SET NULL,
     lower_garment_id UUID REFERENCES garments(id) ON DELETE SET NULL,
     footwear_id UUID REFERENCES garments(id) ON DELETE SET NULL,

     -- Imagen generada
     generated_image_url TEXT NOT NULL, -- Almacenada en Supabase Storage
     thumbnail_url TEXT,

     -- Metadata de generación
     model_used TEXT NOT NULL, -- 'gemini-3-pro' o 'gemini-2.5-flash'
     generation_time_ms INTEGER,

     is_favorite BOOLEAN DEFAULT false,

     created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_tryon_results_user_id ON tryon_results(user_id);
CREATE INDEX idx_tryon_results_outfit_id ON tryon_results(outfit_id);

1.2 Row Level Security (RLS)

-- Habilitar RLS en todas las tablas
ALTER TABLE garments ENABLE ROW LEVEL SECURITY;
ALTER TABLE outfits ENABLE ROW LEVEL SECURITY;
ALTER TABLE tryon_results ENABLE ROW LEVEL SECURITY;

-- Políticas para garments
CREATE POLICY "Users can view their own garments"
ON garments FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own garments"
ON garments FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own garments"
ON garments FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own garments"
ON garments FOR DELETE
USING (auth.uid() = user_id);

-- Políticas para outfits (similar)
CREATE POLICY "Users can view their own outfits"
ON outfits FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own outfits"
ON outfits FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own outfits"
ON outfits FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own outfits"
ON outfits FOR DELETE
USING (auth.uid() = user_id);

-- Políticas para tryon_results (similar)
CREATE POLICY "Users can view their own tryon results"
ON tryon_results FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own tryon results"
ON tryon_results FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own tryon results"
ON tryon_results FOR DELETE
USING (auth.uid() = user_id);

1.3 Supabase Storage Buckets

-- Crear buckets para imágenes
INSERT INTO storage.buckets (id, name, public)
VALUES
('garments', 'garments', true),
('tryon-results', 'tryon-results', false),
('user-photos', 'user-photos', false);

-- Políticas de storage para garments (público)
CREATE POLICY "Garment images are publicly accessible"
ON storage.objects FOR SELECT
USING (bucket_id = 'garments');

CREATE POLICY "Users can upload their own garment images"
ON storage.objects FOR INSERT
WITH CHECK (
bucket_id = 'garments' AND
auth.uid()::text = (storage.foldername(name))[1]
);

CREATE POLICY "Users can delete their own garment images"
ON storage.objects FOR DELETE
USING (
bucket_id = 'garments' AND
auth.uid()::text = (storage.foldername(name))[1]
);

-- Políticas de storage para tryon-results (privado)
CREATE POLICY "Users can view their own tryon results"
ON storage.objects FOR SELECT
USING (
bucket_id = 'tryon-results' AND
auth.uid()::text = (storage.foldername(name))[1]
);

CREATE POLICY "Users can upload their own tryon results"
ON storage.objects FOR INSERT
WITH CHECK (
bucket_id = 'tryon-results' AND
auth.uid()::text = (storage.foldername(name))[1]
);

-- Similar para user-photos
CREATE POLICY "Users can view their own photos"
ON storage.objects FOR SELECT
USING (
bucket_id = 'user-photos' AND
auth.uid()::text = (storage.foldername(name))[1]
);

CREATE POLICY "Users can upload their own photos"
ON storage.objects FOR INSERT
WITH CHECK (
bucket_id = 'user-photos' AND
auth.uid()::text = (storage.foldername(name))[1]
);

1.4 Seed Data (Categorías iniciales)

INSERT INTO categories (name, display_name, icon) VALUES
('upper', 'Parte Superior', 'shirt'),
('lower', 'Parte Inferior', 'pants'),
('footwear', 'Calzado', 'shoe'),
('accessories', 'Accesorios', 'watch');

Archivos a crear:
- database/schema.sql (en la raíz del proyecto, para documentación)
- Ejecutar scripts directamente en Supabase Dashboard SQL Editor

 ---
FASE 2: Feature Wardrobe - Data Layer

Objetivo: Implementar capa de datos para gestión de prendas usando Supabase KT

2.1 Domain Models

Archivo: features/wardrobe/domain/model/Category.kt
data class Category(
val id: String,
val name: String, // 'upper', 'lower', 'footwear'
val displayName: String,
val icon: String?,
val createdAt: Long
)

Archivo: features/wardrobe/domain/model/Garment.kt
data class Garment(
val id: String,
val userId: String,
val categoryId: String,
val name: String,
val description: String?,
val type: String, // 't-shirt', 'jeans', etc.
val color: String?,
val brand: String?,
val fit: GarmentFit,
val imageUrl: String,
val thumbnailUrl: String?,
val isFavorite: Boolean,
val wearCount: Int,
val createdAt: Long,
val updatedAt: Long
)

enum class GarmentFit {
TIGHT, REGULAR, LOOSE;

     companion object {
         fun fromString(value: String): GarmentFit {
             return when (value.lowercase()) {
                 "tight" -> TIGHT
                 "loose" -> LOOSE
                 else -> REGULAR
             }
         }
     }
}

Archivo: features/wardrobe/domain/model/Outfit.kt
data class Outfit(
val id: String,
val userId: String,
val name: String,
val description: String?,
val upperGarmentId: String?,
val lowerGarmentId: String?,
val footwearId: String?,
val isFavorite: Boolean,
val createdAt: Long,
val updatedAt: Long
)

2.2 DTOs (Data Transfer Objects)

Archivo: features/wardrobe/data/remote/dto/CategoryDto.kt
@Serializable
data class CategoryDto(
val id: String,
val name: String,
@SerialName("display_name")
val displayName: String,
val icon: String? = null,
@SerialName("created_at")
val createdAt: String
)

Archivo: features/wardrobe/data/remote/dto/GarmentDto.kt
@Serializable
data class GarmentDto(
val id: String,
@SerialName("user_id")
val userId: String,
@SerialName("category_id")
val categoryId: String,
val name: String,
val description: String? = null,
val type: String,
val color: String? = null,
val brand: String? = null,
val fit: String,
@SerialName("image_url")
val imageUrl: String,
@SerialName("thumbnail_url")
val thumbnailUrl: String? = null,
@SerialName("is_favorite")
val isFavorite: Boolean,
@SerialName("wear_count")
val wearCount: Int,
@SerialName("created_at")
val createdAt: String,
@SerialName("updated_at")
val updatedAt: String
)

2.3 Mappers

Archivo: features/wardrobe/data/mapper/WardrobeMappers.kt
fun CategoryDto.toDomain(): Category {
return Category(
id = id,
name = name,
displayName = displayName,
icon = icon,
createdAt = Instant.parse(createdAt).toEpochMilliseconds()
)
}

fun GarmentDto.toDomain(): Garment {
return Garment(
id = id,
userId = userId,
categoryId = categoryId,
name = name,
description = description,
type = type,
color = color,
brand = brand,
fit = GarmentFit.fromString(fit),
imageUrl = imageUrl,
thumbnailUrl = thumbnailUrl,
isFavorite = isFavorite,
wearCount = wearCount,
createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
updatedAt = Instant.parse(updatedAt).toEpochMilliseconds()
)
}

fun Garment.toDto(): GarmentDto {
return GarmentDto(
id = id,
userId = userId,
categoryId = categoryId,
name = name,
description = description,
type = type,
color = color,
brand = brand,
fit = fit.name.lowercase(),
imageUrl = imageUrl,
thumbnailUrl = thumbnailUrl,
isFavorite = isFavorite,
wearCount = wearCount,
createdAt = Instant.fromEpochMilliseconds(createdAt).toString(),
updatedAt = Instant.fromEpochMilliseconds(updatedAt).toString()
)
}

2.4 Repository Interface

Archivo: features/wardrobe/domain/repository/WardrobeRepository.kt
interface WardrobeRepository {
// Categories
suspend fun getCategories(): Result<List<Category>>

     // Garments
     suspend fun getGarments(categoryId: String? = null): Result<List<Garment>>
     suspend fun getGarmentById(id: String): Result<Garment>
     suspend fun createGarment(
         categoryId: String,
         name: String,
         description: String?,
         type: String,
         color: String?,
         brand: String?,
         fit: GarmentFit,
         imageBytes: ByteArray
     ): Result<Garment>
     suspend fun updateGarment(garment: Garment): Result<Garment>
     suspend fun deleteGarment(id: String): Result<Unit>
     suspend fun toggleFavorite(id: String): Result<Garment>

     // Image upload
     suspend fun uploadGarmentImage(userId: String, imageBytes: ByteArray): Result<String>
     suspend fun deleteGarmentImage(imageUrl: String): Result<Unit>
}

2.5 Repository Implementation

Archivo: features/wardrobe/data/repository/WardrobeRepositoryImpl.kt
class WardrobeRepositoryImpl(
private val supabase: SupabaseClient
) : WardrobeRepository {

     override suspend fun getCategories(): Result<List<Category>> {
         return try {
             val categories = supabase.from("categories")
                 .select()
                 .decodeList<CategoryDto>()
                 .map { it.toDomain() }
             Result.success(categories)
         } catch (e: RestException) {
             Result.failure(Exception("Error al obtener categorías: ${e.message}"))
         }
     }

     override suspend fun getGarments(categoryId: String?): Result<List<Garment>> {
         return try {
             val query = supabase.from("garments").select()

             val garments = if (categoryId != null) {
                 query.decodeList<GarmentDto> {
                     filter {
                         GarmentDto::categoryId eq categoryId
                     }
                 }
             } else {
                 query.decodeList<GarmentDto>()
             }

             Result.success(garments.map { it.toDomain() })
         } catch (e: RestException) {
             Result.failure(Exception("Error al obtener prendas: ${e.message}"))
         }
     }

     override suspend fun createGarment(
         categoryId: String,
         name: String,
         description: String?,
         type: String,
         color: String?,
         brand: String?,
         fit: GarmentFit,
         imageBytes: ByteArray
     ): Result<Garment> {
         return try {
             // 1. Upload image to Storage
             val userId = supabase.auth.currentUserOrNull()?.id
                 ?: return Result.failure(Exception("Usuario no autenticado"))

             val uploadResult = uploadGarmentImage(userId, imageBytes)
             val imageUrl = uploadResult.getOrElse {
                 return Result.failure(Exception("Error al subir imagen"))
             }

             // 2. Create garment in database
             val garmentDto = supabase.from("garments")
                 .insert(buildJsonObject {
                     put("user_id", userId)
                     put("category_id", categoryId)
                     put("name", name)
                     description?.let { put("description", it) }
                     put("type", type)
                     color?.let { put("color", it) }
                     brand?.let { put("brand", it) }
                     put("fit", fit.name.lowercase())
                     put("image_url", imageUrl)
                 }) {
                     select()
                 }
                 .decodeSingle<GarmentDto>()

             Result.success(garmentDto.toDomain())
         } catch (e: RestException) {
             Result.failure(Exception("Error al crear prenda: ${e.message}"))
         }
     }

     override suspend fun uploadGarmentImage(
         userId: String,
         imageBytes: ByteArray
     ): Result<String> {
         return try {
             val fileName = "${UUID.randomUUID()}.jpg"
             val filePath = "$userId/$fileName"

             supabase.storage["garments"].upload(filePath, imageBytes) {
                 upsert = false
                 contentType = "image/jpeg"
             }

             val imageUrl = supabase.storage["garments"].publicUrl(filePath)
             Result.success(imageUrl)
         } catch (e: RestException) {
             Result.failure(Exception("Error al subir imagen: ${e.message}"))
         }
     }

     override suspend fun updateGarment(garment: Garment): Result<Garment> {
         return try {
             val updated = supabase.from("garments")
                 .update(buildJsonObject {
                     put("name", garment.name)
                     garment.description?.let { put("description", it) }
                     put("type", garment.type)
                     garment.color?.let { put("color", it) }
                     garment.brand?.let { put("brand", it) }
                     put("fit", garment.fit.name.lowercase())
                     put("is_favorite", garment.isFavorite)
                 }) {
                     filter {
                         GarmentDto::id eq garment.id
                     }
                     select()
                 }
                 .decodeSingle<GarmentDto>()

             Result.success(updated.toDomain())
         } catch (e: RestException) {
             Result.failure(Exception("Error al actualizar prenda: ${e.message}"))
         }
     }

     override suspend fun deleteGarment(id: String): Result<Unit> {
         return try {
             // Get garment to delete its image
             val garment = getGarmentById(id).getOrNull()

             // Delete from database
             supabase.from("garments").delete {
                 filter {
                     GarmentDto::id eq id
                 }
             }

             // Delete image from storage
             garment?.let { deleteGarmentImage(it.imageUrl) }

             Result.success(Unit)
         } catch (e: RestException) {
             Result.failure(Exception("Error al eliminar prenda: ${e.message}"))
         }
     }

     override suspend fun toggleFavorite(id: String): Result<Garment> {
         return try {
             val current = getGarmentById(id).getOrThrow()
             val updated = supabase.from("garments")
                 .update(buildJsonObject {
                     put("is_favorite", !current.isFavorite)
                 }) {
                     filter {
                         GarmentDto::id eq id
                     }
                     select()
                 }
                 .decodeSingle<GarmentDto>()

             Result.success(updated.toDomain())
         } catch (e: RestException) {
             Result.failure(Exception("Error al marcar favorito: ${e.message}"))
         }
     }
}

Archivos clave en esta fase:
- features/wardrobe/domain/model/*.kt
- features/wardrobe/domain/repository/WardrobeRepository.kt
- features/wardrobe/data/remote/dto/*.kt
- features/wardrobe/data/mapper/WardrobeMappers.kt
- features/wardrobe/data/repository/WardrobeRepositoryImpl.kt

 ---
FASE 3: Feature Wardrobe - Domain Layer (Use Cases)

Objetivo: Crear casos de uso para operaciones de negocio del guardarropa

3.1 Use Cases

Archivo: features/wardrobe/domain/usecase/GetCategoriesUseCase.kt
class GetCategoriesUseCase(
private val repository: WardrobeRepository
) {
suspend operator fun invoke(): Result<List<Category>> {
return repository.getCategories()
}
}

Archivo: features/wardrobe/domain/usecase/GetGarmentsUseCase.kt
class GetGarmentsUseCase(
private val repository: WardrobeRepository
) {
suspend operator fun invoke(
categoryId: String? = null,
favoritesOnly: Boolean = false
): Result<List<Garment>> {
return repository.getGarments(categoryId).map { garments ->
if (favoritesOnly) {
garments.filter { it.isFavorite }
} else {
garments
}
}
}
}

Archivo: features/wardrobe/domain/usecase/AddGarmentUseCase.kt
class AddGarmentUseCase(
private val repository: WardrobeRepository
) {
suspend operator fun invoke(
categoryId: String,
name: String,
description: String?,
type: String,
color: String?,
brand: String?,
fit: GarmentFit,
imageBytes: ByteArray
): Result<Garment> {
// Validations
if (name.isBlank()) {
return Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
}

         if (imageBytes.isEmpty()) {
             return Result.failure(IllegalArgumentException("Debe proporcionar una imagen"))
         }

         return repository.createGarment(
             categoryId, name, description, type, color, brand, fit, imageBytes
         )
     }
}

Archivo: features/wardrobe/domain/usecase/DeleteGarmentUseCase.kt
class DeleteGarmentUseCase(
private val repository: WardrobeRepository
) {
suspend operator fun invoke(garmentId: String): Result<Unit> {
return repository.deleteGarment(garmentId)
}
}

Archivo: features/wardrobe/domain/usecase/ToggleFavoriteUseCase.kt
class ToggleFavoriteUseCase(
private val repository: WardrobeRepository
) {
suspend operator fun invoke(garmentId: String): Result<Garment> {
return repository.toggleFavorite(garmentId)
}
}

 ---
FASE 4: Feature Wardrobe - Presentation Layer (UI)

Objetivo: Crear pantallas para visualizar y gestionar el guardarropa

4.1 ViewModel y State

Archivo: features/wardrobe/presentation/WardrobeState.kt
data class WardrobeState(
val categories: List<Category> = emptyList(),
val garments: List<Garment> = emptyList(),
val selectedCategoryId: String? = null,
val isLoading: Boolean = false,
val error: String? = null,
val showFavoritesOnly: Boolean = false
)

Archivo: features/wardrobe/presentation/WardrobeViewModel.kt
class WardrobeViewModel(
private val getCategoriesUseCase: GetCategoriesUseCase,
private val getGarmentsUseCase: GetGarmentsUseCase,
private val deleteGarmentUseCase: DeleteGarmentUseCase,
private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

     private val _state = MutableStateFlow(WardrobeState())
     val state: StateFlow<WardrobeState> = _state.asStateFlow()

     init {
         loadCategories()
         loadGarments()
     }

     fun loadCategories() {
         viewModelScope.launch {
             getCategoriesUseCase().fold(
                 onSuccess = { categories ->
                     _state.update { it.copy(categories = categories) }
                 },
                 onFailure = { error ->
                     _state.update { it.copy(error = error.message) }
                 }
             )
         }
     }

     fun loadGarments(categoryId: String? = null) {
         viewModelScope.launch {
             _state.update { it.copy(isLoading = true, error = null) }

             getGarmentsUseCase(
                 categoryId = categoryId,
                 favoritesOnly = _state.value.showFavoritesOnly
             ).fold(
                 onSuccess = { garments ->
                     _state.update {
                         it.copy(
                             garments = garments,
                             selectedCategoryId = categoryId,
                             isLoading = false
                         )
                     }
                 },
                 onFailure = { error ->
                     _state.update {
                         it.copy(
                             error = error.message,
                             isLoading = false
                         )
                     }
                 }
             )
         }
     }

     fun toggleFavorite(garmentId: String) {
         viewModelScope.launch {
             toggleFavoriteUseCase(garmentId).fold(
                 onSuccess = { updatedGarment ->
                     _state.update { state ->
                         state.copy(
                             garments = state.garments.map {
                                 if (it.id == updatedGarment.id) updatedGarment else it
                             }
                         )
                     }
                 },
                 onFailure = { error ->
                     _state.update { it.copy(error = error.message) }
                 }
             )
         }
     }

     fun deleteGarment(garmentId: String) {
         viewModelScope.launch {
             deleteGarmentUseCase(garmentId).fold(
                 onSuccess = {
                     _state.update { state ->
                         state.copy(
                             garments = state.garments.filter { it.id != garmentId }
                         )
                     }
                 },
                 onFailure = { error ->
                     _state.update { it.copy(error = error.message) }
                 }
             )
         }
     }

     fun toggleFavoritesFilter() {
         _state.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
         loadGarments(_state.value.selectedCategoryId)
     }
}

4.2 Screens

Archivo: features/wardrobe/presentation/WardrobeScreen.kt
@Composable
fun WardrobeScreen(
onNavigateToAddGarment: () -> Unit,
onNavigateToTryOn: () -> Unit,
viewModel: WardrobeViewModel = koinInject()
) {
val state by viewModel.state.collectAsState()

     Scaffold(
         topBar = {
             TopAppBar(
                 title = { Text("Mi Guardarropa") },
                 actions = {
                     IconButton(onClick = { viewModel.toggleFavoritesFilter() }) {
                         Icon(
                             imageVector = if (state.showFavoritesOnly)
                                 Icons.Filled.Favorite
                             else
                                 Icons.Outlined.FavoriteBorder,
                             contentDescription = "Favoritos"
                         )
                     }
                 }
             )
         },
         floatingActionButton = {
             Column {
                 FloatingActionButton(
                     onClick = onNavigateToTryOn,
                     modifier = Modifier.padding(bottom = 16.dp)
                 ) {
                     Icon(Icons.Default.PhotoCamera, "Probador Virtual")
                 }
                 FloatingActionButton(onClick = onNavigateToAddGarment) {
                     Icon(Icons.Default.Add, "Agregar Prenda")
                 }
             }
         }
     ) { padding ->
         Column(
             modifier = Modifier
                 .fillMaxSize()
                 .padding(padding)
         ) {
             // Category tabs
             ScrollableTabRow(
                 selectedTabIndex = state.categories.indexOfFirst {
                     it.id == state.selectedCategoryId
                 }.coerceAtLeast(0)
             ) {
                 Tab(
                     selected = state.selectedCategoryId == null,
                     onClick = { viewModel.loadGarments(null) },
                     text = { Text("Todas") }
                 )

                 state.categories.forEach { category ->
                     Tab(
                         selected = state.selectedCategoryId == category.id,
                         onClick = { viewModel.loadGarments(category.id) },
                         text = { Text(category.displayName) }
                     )
                 }
             }

             // Garments grid
             when {
                 state.isLoading -> {
                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator()
                     }
                 }
                 state.error != null -> {
                     ErrorView(message = state.error!!)
                 }
                 state.garments.isEmpty() -> {
                     EmptyStateView(message = "No tienes prendas aún")
                 }
                 else -> {
                     LazyVerticalGrid(
                         columns = GridCells.Fixed(2),
                         contentPadding = PaddingValues(16.dp),
                         horizontalArrangement = Arrangement.spacedBy(16.dp),
                         verticalArrangement = Arrangement.spacedBy(16.dp)
                     ) {
                         items(state.garments) { garment ->
                             GarmentCard(
                                 garment = garment,
                                 onFavoriteClick = { viewModel.toggleFavorite(garment.id) },
                                 onDeleteClick = { viewModel.deleteGarment(garment.id) }
                             )
                         }
                     }
                 }
             }
         }
     }
}

@Composable
fun GarmentCard(
garment: Garment,
onFavoriteClick: () -> Unit,
onDeleteClick: () -> Unit
) {
Card(
modifier = Modifier
.fillMaxWidth()
.aspectRatio(0.75f)
) {
Box {
// Image
AsyncImage(
model = garment.imageUrl,
contentDescription = garment.name,
modifier = Modifier.fillMaxSize(),
contentScale = ContentScale.Crop
)

             // Favorite button
             IconButton(
                 onClick = onFavoriteClick,
                 modifier = Modifier.align(Alignment.TopEnd)
             ) {
                 Icon(
                     imageVector = if (garment.isFavorite)
                         Icons.Filled.Favorite
                     else
                         Icons.Outlined.FavoriteBorder,
                     contentDescription = "Favorito",
                     tint = if (garment.isFavorite) Color.Red else Color.White
                 )
             }

             // Info overlay
             Column(
                 modifier = Modifier
                     .align(Alignment.BottomStart)
                     .background(
                         Brush.verticalGradient(
                             colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                         )
                     )
                     .fillMaxWidth()
                     .padding(8.dp)
             ) {
                 Text(
                     text = garment.name,
                     style = MaterialTheme.typography.titleSmall,
                     color = Color.White,
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                 )
                 garment.brand?.let {
                     Text(
                         text = it,
                         style = MaterialTheme.typography.bodySmall,
                         color = Color.White.copy(alpha = 0.8f)
                     )
                 }
             }
         }
     }
}

 ---
FASE 5: Feature Virtual Try-On - Data Layer

Objetivo: Implementar integración con Google Gemini AI desde KMP

5.1 Configuración

Agregar en local.properties:
google.gemini.api.key=YOUR_GEMINI_API_KEY

Agregar en sharedUI/build.gradle.kts:
buildConfigField("String", "GEMINI_API_KEY", localProperties["google.gemini.api.key"] ?: "")

Dependencias adicionales:
// En libs.versions.toml
[versions]
ktor = "3.3.3"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

5.2 Domain Models

Archivo: features/tryon/domain/model/TryOnRequest.kt
data class TryOnRequest(
val userImageBytes: ByteArray,
val upperGarment: GarmentForTryOn? = null,
val lowerGarment: GarmentForTryOn? = null,
val footwear: GarmentForTryOn? = null,
val modelVersion: GeminiModel = GeminiModel.GEMINI_3_PRO
)

data class GarmentForTryOn(
val garmentId: String,
val imageBytes: ByteArray,
val type: String,
val description: String,
val fit: String
)

enum class GeminiModel(val modelName: String, val apiPath: String) {
GEMINI_3_PRO(
"Gemini 3 Pro Image Preview",
"gemini-3-pro-image-preview:generateContent"
),
GEMINI_2_5_FLASH(
"Gemini 2.5 Flash Image",
"gemini-2.5-flash-image:generateContent"
)
}

Archivo: features/tryon/domain/model/TryOnResult.kt
data class TryOnResult(
val id: String,
val userId: String,
val outfitId: String?,
val upperGarmentId: String?,
val lowerGarmentId: String?,
val footwearId: String?,
val generatedImageUrl: String,
val thumbnailUrl: String?,
val modelUsed: String,
val generationTimeMs: Int,
val isFavorite: Boolean,
val createdAt: Long
)

5.3 DTOs para Gemini API

Archivo: features/tryon/data/remote/dto/GeminiRequestDto.kt
@Serializable
data class GeminiRequestDto(
val contents: List<ContentDto>,
val generationConfig: GenerationConfigDto? = null
)

@Serializable
data class ContentDto(
val parts: List<PartDto>
)

@Serializable
data class PartDto(
val text: String? = null,
@SerialName("inline_data")
val inlineData: InlineDataDto? = null
)

@Serializable
data class InlineDataDto(
@SerialName("mime_type")
val mimeType: String,
val data: String // Base64
)

@Serializable
data class GenerationConfigDto(
val responseModalities: List<String>,
val imageConfig: ImageConfigDto
)

@Serializable
data class ImageConfigDto(
val aspectRatio: String,
val imageSize: String
)

Archivo: features/tryon/data/remote/dto/GeminiResponseDto.kt
@Serializable
data class GeminiResponseDto(
val candidates: List<CandidateDto>
)

@Serializable
data class CandidateDto(
val content: ResponseContentDto
)

@Serializable
data class ResponseContentDto(
val parts: List<ResponsePartDto>
)

@Serializable
data class ResponsePartDto(
val text: String? = null,
@SerialName("inline_data")
val inlineData: InlineDataDto? = null,
@SerialName("inlineData") // Fallback
val inlineDataCamelCase: InlineDataDto? = null
)

5.4 Repository Interface

Archivo: features/tryon/domain/repository/TryOnRepository.kt
interface TryOnRepository {
// AI Generation
suspend fun generateTryOnImage(request: TryOnRequest): Result<ByteArray>

     // Storage
     suspend fun saveTryOnResult(
         userId: String,
         imageBytes: ByteArray,
         outfitId: String?,
         upperGarmentId: String?,
         lowerGarmentId: String?,
         footwearId: String?,
         modelUsed: String,
         generationTimeMs: Int
     ): Result<TryOnResult>

     suspend fun getTryOnResults(userId: String): Result<List<TryOnResult>>
     suspend fun deleteTryOnResult(resultId: String): Result<Unit>
     suspend fun toggleFavorite(resultId: String): Result<TryOnResult>
}

5.5 Repository Implementation

Archivo: features/tryon/data/repository/TryOnRepositoryImpl.kt
class TryOnRepositoryImpl(
private val supabase: SupabaseClient,
private val httpClient: HttpClient
) : TryOnRepository {

     companion object {
         private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
     }

     override suspend fun generateTryOnImage(request: TryOnRequest): Result<ByteArray> {
         return try {
             val startTime = System.currentTimeMillis()

             // Build prompt
             val prompt = buildTryOnPrompt(request)

             // Build content parts
             val parts = mutableListOf<PartDto>()

             // 1. Text prompt first
             parts.add(PartDto(text = prompt))

             // 2. User image
             parts.add(PartDto(
                 inlineData = InlineDataDto(
                     mimeType = "image/jpeg",
                     data = request.userImageBytes.toBase64()
                 )
             ))

             // 3. Garment images
             request.upperGarment?.let {
                 parts.add(PartDto(
                     inlineData = InlineDataDto(
                         mimeType = "image/jpeg",
                         data = it.imageBytes.toBase64()
                     )
                 ))
             }

             request.lowerGarment?.let {
                 parts.add(PartDto(
                     inlineData = InlineDataDto(
                         mimeType = "image/jpeg",
                         data = it.imageBytes.toBase64()
                     )
                 ))
             }

             request.footwear?.let {
                 parts.add(PartDto(
                     inlineData = InlineDataDto(
                         mimeType = "image/jpeg",
                         data = it.imageBytes.toBase64()
                     )
                 ))
             }

             // Build request
             val requestBody = if (request.modelVersion == GeminiModel.GEMINI_3_PRO) {
                 GeminiRequestDto(
                     contents = listOf(ContentDto(parts = parts)),
                     generationConfig = GenerationConfigDto(
                         responseModalities = listOf("TEXT", "IMAGE"),
                         imageConfig = ImageConfigDto(
                             aspectRatio = "3:4",
                             imageSize = "2K"
                         )
                     )
                 )
             } else {
                 GeminiRequestDto(
                     contents = listOf(ContentDto(parts = parts))
                 )
             }

             // Make API call
             val response = httpClient.post("$GEMINI_BASE_URL/${request.modelVersion.apiPath}") {
                 header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                 header("Content-Type", "application/json")
                 setBody(requestBody)
             }

             if (!response.status.isSuccess()) {
                 val errorText = response.bodyAsText()
                 return Result.failure(Exception("Gemini API error: ${response.status} - $errorText"))
             }

             val geminiResponse = response.body<GeminiResponseDto>()

             // Extract image
             val imagePart = geminiResponse.candidates.firstOrNull()
                 ?.content
                 ?.parts
                 ?.firstOrNull { it.inlineData != null || it.inlineDataCamelCase != null }

             val imageBase64 = imagePart?.inlineData?.data
                 ?: imagePart?.inlineDataCamelCase?.data
                 ?: return Result.failure(Exception("No se generó ninguna imagen"))

             val imageBytes = imageBase64.fromBase64ToByteArray()

             Result.success(imageBytes)
         } catch (e: Exception) {
             Result.failure(Exception("Error al generar imagen: ${e.message}"))
         }
     }

     override suspend fun saveTryOnResult(
         userId: String,
         imageBytes: ByteArray,
         outfitId: String?,
         upperGarmentId: String?,
         lowerGarmentId: String?,
         footwearId: String?,
         modelUsed: String,
         generationTimeMs: Int
     ): Result<TryOnResult> {
         return try {
             // Upload image to Storage
             val fileName = "${UUID.randomUUID()}.jpg"
             val filePath = "$userId/$fileName"

             supabase.storage["tryon-results"].upload(filePath, imageBytes) {
                 upsert = false
                 contentType = "image/jpeg"
             }

             val imageUrl = supabase.storage["tryon-results"].createSignedUrl(
                 filePath,
                 expiresIn = 365.days
             )

             // Save to database
             val result = supabase.from("tryon_results")
                 .insert(buildJsonObject {
                     put("user_id", userId)
                     outfitId?.let { put("outfit_id", it) }
                     upperGarmentId?.let { put("upper_garment_id", it) }
                     lowerGarmentId?.let { put("lower_garment_id", it) }
                     footwearId?.let { put("footwear_id", it) }
                     put("generated_image_url", imageUrl)
                     put("model_used", modelUsed)
                     put("generation_time_ms", generationTimeMs)
                 }) {
                     select()
                 }
                 .decodeSingle<TryOnResultDto>()

             Result.success(result.toDomain())
         } catch (e: RestException) {
             Result.failure(Exception("Error al guardar resultado: ${e.message}"))
         }
     }

     private fun buildTryOnPrompt(request: TryOnRequest): String {
         val garmentInstructions = buildString {
             append("PRENDAS A COLOCAR:\n")
             var imageIndex = 2

             request.upperGarment?.let {
                 append("- PRENDA SUPERIOR (de la Imagen $imageIndex): ${it.description}\n")
                 append("  - Ajuste: ${it.fit}\n")
                 append("  - Ubicación: Torso y brazos\n\n")
                 imageIndex++
             }

             request.lowerGarment?.let {
                 append("- PRENDA INFERIOR (de la Imagen $imageIndex): ${it.description}\n")
                 append("  - Ajuste: ${it.fit}\n")
                 append("  - Ubicación: Piernas\n\n")
                 imageIndex++
             }

             request.footwear?.let {
                 append("- CALZADO (de la Imagen $imageIndex): ${it.description}\n")
                 append("  - Ajuste: ${it.fit}\n")
                 append("  - Ubicación: Pies\n")
             }
         }

         return """
             Actúa como un fotógrafo de moda profesional y editor experto especializado en virtual try-on.

             TAREA: Crea una imagen fotorrealista de la persona en la [Imagen 1] vistiendo las prendas especificadas.

             $garmentInstructions

             REQUISITOS CRÍTICOS - ORDEN DE PRIORIDAD:

             1. PRESERVACIÓN DE IDENTIDAD (MÁXIMA PRIORIDAD):
                 - NO ALTERES la cara, rasgos faciales, expresión, ni identidad de la persona
                 - Mantén EXACTAMENTE el mismo rostro, ojos, nariz, boca, cejas de la [Imagen 1]
                 - Conserva el mismo tono de piel, textura y características faciales
                 - NO modifiques el peinado, color de cabello ni accesorios faciales
                 - La persona debe ser 100% reconocible como la misma de la foto original

             2. PRESERVACIÓN DEL CUERPO:
                 - Mantén la misma pose, postura y posición del cuerpo
                 - Conserva el mismo tipo de cuerpo y proporciones
                 - NO alteres la altura, complexión ni estructura corporal

             3. APLICACIÓN DE PRENDAS:
                 - Coloca las prendas especificadas sobre el cuerpo de la persona
                 - Las prendas deben adaptarse naturalmente al cuerpo
                 - Respeta la física de la tela: pliegues, caídas, arrugas naturales

             4. INTEGRACIÓN VISUAL:
                 - Mantén la misma iluminación y sombras del entorno original
                 - Las sombras de las prendas deben coincidir con la luz de la [Imagen 1]
                 - Conserva el mismo fondo y contexto de la foto original
                 - El resultado debe parecer una foto real, no un montaje

             Genera solo la imagen final sin texto adicional.
         """.trimIndent()
     }
}

Archivos clave en esta fase:
- features/tryon/domain/model/*.kt
- features/tryon/domain/repository/TryOnRepository.kt
- features/tryon/data/remote/dto/*.kt
- features/tryon/data/repository/TryOnRepositoryImpl.kt

 ---
FASE 6: Feature Virtual Try-On - Domain & Presentation

Objetivo: Crear UI para seleccionar prendas y generar try-ons

6.1 Use Cases

Archivo: features/tryon/domain/usecase/GenerateTryOnUseCase.kt
class GenerateTryOnUseCase(
private val tryOnRepository: TryOnRepository
) {
suspend operator fun invoke(request: TryOnRequest): Result<ByteArray> {
// Validations
if (request.userImageBytes.isEmpty()) {
return Result.failure(IllegalArgumentException("Debe proporcionar una foto"))
}

         if (request.upperGarment == null &&
             request.lowerGarment == null &&
             request.footwear == null) {
             return Result.failure(IllegalArgumentException("Debe seleccionar al menos una prenda"))
         }

         return tryOnRepository.generateTryOnImage(request)
     }
}

Archivo: features/tryon/domain/usecase/SaveTryOnResultUseCase.kt
class SaveTryOnResultUseCase(
private val tryOnRepository: TryOnRepository
) {
suspend operator fun invoke(
userId: String,
imageBytes: ByteArray,
outfitId: String?,
upperGarmentId: String?,
lowerGarmentId: String?,
footwearId: String?,
modelUsed: String,
generationTimeMs: Int
): Result<TryOnResult> {
return tryOnRepository.saveTryOnResult(
userId, imageBytes, outfitId, upperGarmentId,
lowerGarmentId, footwearId, modelUsed, generationTimeMs
)
}
}

6.2 ViewModel

Archivo: features/tryon/presentation/TryOnState.kt
data class TryOnState(
val userImageBytes: ByteArray? = null,
val selectedUpperGarment: Garment? = null,
val selectedLowerGarment: Garment? = null,
val selectedFootwear: Garment? = null,
val selectedModel: GeminiModel = GeminiModel.GEMINI_3_PRO,
val isGenerating: Boolean = false,
val generatedImageBytes: ByteArray? = null,
val error: String? = null,
val progress: Float = 0f
)

Archivo: features/tryon/presentation/TryOnViewModel.kt
class TryOnViewModel(
private val generateTryOnUseCase: GenerateTryOnUseCase,
private val saveTryOnResultUseCase: SaveTryOnResultUseCase
) : ViewModel() {

     private val _state = MutableStateFlow(TryOnState())
     val state: StateFlow<TryOnState> = _state.asStateFlow()

     fun setUserImage(imageBytes: ByteArray) {
         _state.update { it.copy(userImageBytes = imageBytes) }
     }

     fun selectUpperGarment(garment: Garment?) {
         _state.update { it.copy(selectedUpperGarment = garment) }
     }

     fun selectLowerGarment(garment: Garment?) {
         _state.update { it.copy(selectedLowerGarment = garment) }
     }

     fun selectFootwear(garment: Garment?) {
         _state.update { it.copy(selectedFootwear = garment) }
     }

     fun selectModel(model: GeminiModel) {
         _state.update { it.copy(selectedModel = model) }
     }

     fun generateTryOn() {
         val currentState = _state.value

         if (currentState.userImageBytes == null) {
             _state.update { it.copy(error = "Primero toma o selecciona una foto") }
             return
         }

         viewModelScope.launch {
             _state.update { it.copy(isGenerating = true, error = null, progress = 0.1f) }

             try {
                 // Prepare garments
                 val upperGarment = currentState.selectedUpperGarment?.let {
                     GarmentForTryOn(
                         garmentId = it.id,
                         imageBytes = downloadImage(it.imageUrl),
                         type = it.type,
                         description = "${it.name} - ${it.color ?: ""}",
                         fit = it.fit.name.lowercase()
                     )
                 }

                 _state.update { it.copy(progress = 0.3f) }

                 val lowerGarment = currentState.selectedLowerGarment?.let {
                     GarmentForTryOn(
                         garmentId = it.id,
                         imageBytes = downloadImage(it.imageUrl),
                         type = it.type,
                         description = "${it.name} - ${it.color ?: ""}",
                         fit = it.fit.name.lowercase()
                     )
                 }

                 _state.update { it.copy(progress = 0.5f) }

                 val footwear = currentState.selectedFootwear?.let {
                     GarmentForTryOn(
                         garmentId = it.id,
                         imageBytes = downloadImage(it.imageUrl),
                         type = it.type,
                         description = "${it.name} - ${it.color ?: ""}",
                         fit = it.fit.name.lowercase()
                     )
                 }

                 _state.update { it.copy(progress = 0.7f) }

                 // Generate try-on
                 val request = TryOnRequest(
                     userImageBytes = currentState.userImageBytes,
                     upperGarment = upperGarment,
                     lowerGarment = lowerGarment,
                     footwear = footwear,
                     modelVersion = currentState.selectedModel
                 )

                 val startTime = System.currentTimeMillis()

                 generateTryOnUseCase(request).fold(
                     onSuccess = { generatedImageBytes ->
                         val generationTime = (System.currentTimeMillis() - startTime).toInt()

                         _state.update {
                             it.copy(
                                 generatedImageBytes = generatedImageBytes,
                                 isGenerating = false,
                                 progress = 1f
                             )
                         }

                         // Save result (optional, in background)
                         // saveTryOnResultUseCase(...)
                     },
                     onFailure = { error ->
                         _state.update {
                             it.copy(
                                 error = error.message ?: "Error al generar imagen",
                                 isGenerating = false,
                                 progress = 0f
                             )
                         }
                     }
                 )
             } catch (e: Exception) {
                 _state.update {
                     it.copy(
                         error = e.message ?: "Error inesperado",
                         isGenerating = false,
                         progress = 0f
                     )
                 }
             }
         }
     }

     private suspend fun downloadImage(url: String): ByteArray {
         // TODO: Implementar descarga de imagen desde URL
         // Puede usar Ktor client o Coil
         return byteArrayOf()
     }
}

6.3 Screen

Archivo: features/tryon/presentation/TryOnScreen.kt
@Composable
fun TryOnScreen(
onNavigateBack: () -> Unit,
viewModel: TryOnViewModel = koinInject()
) {
val state by viewModel.state.collectAsState()

     var showGarmentPicker by remember { mutableStateOf<GarmentCategory?>(null) }

     Scaffold(
         topBar = {
             TopAppBar(
                 title = { Text("Probador Virtual") },
                 navigationIcon = {
                     IconButton(onClick = onNavigateBack) {
                         Icon(Icons.Default.ArrowBack, "Volver")
                     }
                 }
             )
         }
     ) { padding ->
         Column(
             modifier = Modifier
                 .fillMaxSize()
                 .padding(padding)
                 .verticalScroll(rememberScrollState())
         ) {
             // User photo section
             Card(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp)
             ) {
                 Column(modifier = Modifier.padding(16.dp)) {
                     Text("Tu foto", style = MaterialTheme.typography.titleMedium)

                     Spacer(modifier = Modifier.height(8.dp))

                     if (state.userImageBytes != null) {
                         Image(
                             bitmap = state.userImageBytes!!.toImageBitmap(),
                             contentDescription = "User photo",
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .height(300.dp),
                             contentScale = ContentScale.Crop
                         )
                     } else {
                         Box(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .height(200.dp)
                                 .background(MaterialTheme.colorScheme.surfaceVariant),
                             contentAlignment = Alignment.Center
                         ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 Icon(Icons.Default.PhotoCamera, null)
                                 Text("Toca para tomar una foto")
                             }
                         }
                     }

                     Spacer(modifier = Modifier.height(8.dp))

                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         Button(
                             onClick = { /* TODO: Take photo */ },
                             modifier = Modifier.weight(1f)
                         ) {
                             Text("Tomar Foto")
                         }
                         OutlinedButton(
                             onClick = { /* TODO: Pick from gallery */ },
                             modifier = Modifier.weight(1f)
                         ) {
                             Text("Galería")
                         }
                     }
                 }
             }

             // Garment selection
             Text(
                 "Seleccionar Prendas",
                 style = MaterialTheme.typography.titleMedium,
                 modifier = Modifier.padding(horizontal = 16.dp)
             )

             Spacer(modifier = Modifier.height(8.dp))

             GarmentSlot(
                 title = "Parte Superior",
                 selectedGarment = state.selectedUpperGarment,
                 onSelectClick = { showGarmentPicker = GarmentCategory.UPPER },
                 onClearClick = { viewModel.selectUpperGarment(null) }
             )

             GarmentSlot(
                 title = "Parte Inferior",
                 selectedGarment = state.selectedLowerGarment,
                 onSelectClick = { showGarmentPicker = GarmentCategory.LOWER },
                 onClearClick = { viewModel.selectLowerGarment(null) }
             )

             GarmentSlot(
                 title = "Calzado",
                 selectedGarment = state.selectedFootwear,
                 onSelectClick = { showGarmentPicker = GarmentCategory.FOOTWEAR },
                 onClearClick = { viewModel.selectFootwear(null) }
             )

             // Model selection
             Card(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp)
             ) {
                 Column(modifier = Modifier.padding(16.dp)) {
                     Text("Modelo de IA", style = MaterialTheme.typography.titleMedium)

                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         FilterChip(
                             selected = state.selectedModel == GeminiModel.GEMINI_3_PRO,
                             onClick = { viewModel.selectModel(GeminiModel.GEMINI_3_PRO) },
                             label = { Text("Alta Calidad") }
                         )
                         FilterChip(
                             selected = state.selectedModel == GeminiModel.GEMINI_2_5_FLASH,
                             onClick = { viewModel.selectModel(GeminiModel.GEMINI_2_5_FLASH) },
                             label = { Text("Rápido") }
                         )
                     }
                 }
             }

             // Generate button
             Button(
                 onClick = { viewModel.generateTryOn() },
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp),
                 enabled = !state.isGenerating &&
                          state.userImageBytes != null &&
                          (state.selectedUpperGarment != null ||
                           state.selectedLowerGarment != null ||
                           state.selectedFootwear != null)
             ) {
                 if (state.isGenerating) {
                     CircularProgressIndicator(
                         modifier = Modifier.size(24.dp),
                         color = MaterialTheme.colorScheme.onPrimary
                     )
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Generando... ${(state.progress * 100).toInt()}%")
                 } else {
                     Text("Generar Imagen")
                 }
             }

             // Generated result
             if (state.generatedImageBytes != null) {
                 Card(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(16.dp)
                 ) {
                     Column {
                         Image(
                             bitmap = state.generatedImageBytes!!.toImageBitmap(),
                             contentDescription = "Generated result",
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .height(400.dp),
                             contentScale = ContentScale.Crop
                         )

                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .padding(16.dp),
                             horizontalArrangement = Arrangement.spacedBy(8.dp)
                         ) {
                             Button(
                                 onClick = { /* TODO: Save */ },
                                 modifier = Modifier.weight(1f)
                             ) {
                                 Text("Guardar")
                             }
                             OutlinedButton(
                                 onClick = { /* TODO: Share */ },
                                 modifier = Modifier.weight(1f)
                             ) {
                                 Text("Compartir")
                             }
                         }
                     }
                 }
             }

             // Error message
             state.error?.let { error ->
                 Text(
                     text = error,
                     color = MaterialTheme.colorScheme.error,
                     modifier = Modifier.padding(16.dp)
                 )
             }
         }
     }
}

@Composable
fun GarmentSlot(
title: String,
selectedGarment: Garment?,
onSelectClick: () -> Unit,
onClearClick: () -> Unit
) {
Card(
modifier = Modifier
.fillMaxWidth()
.padding(horizontal = 16.dp, vertical = 8.dp)
) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(16.dp),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
if (selectedGarment != null) {
Row(
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
AsyncImage(
model = selectedGarment.imageUrl,
contentDescription = selectedGarment.name,
modifier = Modifier
.size(60.dp)
.clip(MaterialTheme.shapes.small),
contentScale = ContentScale.Crop
)
Column {
Text(title, style = MaterialTheme.typography.labelSmall)
Text(selectedGarment.name, style = MaterialTheme.typography.bodyMedium)
}
}
IconButton(onClick = onClearClick) {
Icon(Icons.Default.Close, "Quitar")
}
} else {
Text(title)
TextButton(onClick = onSelectClick) {
Text("Seleccionar")
}
}
}
}
}

 ---
FASE 7: Dependency Injection y Navigation

Objetivo: Integrar las nuevas features en la aplicación

7.1 Koin Modules

Archivo: features/wardrobe/di/WardrobeModule.kt
val wardrobeModule = module {
// Repository
single<WardrobeRepository> {
WardrobeRepositoryImpl(get())
}

     // Use Cases
     factory { GetCategoriesUseCase(get()) }
     factory { GetGarmentsUseCase(get()) }
     factory { AddGarmentUseCase(get()) }
     factory { DeleteGarmentUseCase(get()) }
     factory { ToggleFavoriteUseCase(get()) }

     // ViewModel
     viewModel { WardrobeViewModel(get(), get(), get(), get()) }
}

Archivo: features/tryon/di/TryOnModule.kt
val tryOnModule = module {
// HTTP Client para Gemini
single {
HttpClient {
install(ContentNegotiation) {
json(Json {
ignoreUnknownKeys = true
prettyPrint = true
})
}
}
}

     // Repository
     single<TryOnRepository> {
         TryOnRepositoryImpl(get(), get())
     }

     // Use Cases
     factory { GenerateTryOnUseCase(get()) }
     factory { SaveTryOnResultUseCase(get()) }

     // ViewModel
     viewModel { TryOnViewModel(get(), get()) }
}

Modificar: App.kt
KoinApplication(
application = {
modules(
coreModule,
authenticationModule,
wardrobeModule,  // ✅ Nuevo
tryOnModule      // ✅ Nuevo
)
}
) {
AppTheme(onThemeChanged) {
if (authReady) AppNavigation()
}
}

7.2 Navigation Routes

Modificar: core/presentation/navigation/AppNavigation.kt
sealed class Screen(val route: String) {
data object Login : Screen("login")
data object Register : Screen("register")
data object Home : Screen("home")
data object Wardrobe : Screen("wardrobe")          // ✅ Nuevo
data object AddGarment : Screen("add_garment")     // ✅ Nuevo
data object TryOn : Screen("tryon")                // ✅ Nuevo
}

@Composable
fun AppNavigation(...) {
// ... existing code ...

     NavHost(...) {
         // ... existing routes ...

         composable(Screen.Wardrobe.route) {
             WardrobeScreen(
                 onNavigateToAddGarment = {
                     navController.navigate(Screen.AddGarment.route)
                 },
                 onNavigateToTryOn = {
                     navController.navigate(Screen.TryOn.route)
                 }
             )
         }

         composable(Screen.AddGarment.route) {
             AddGarmentScreen(
                 onNavigateBack = { navController.popBackStack() }
             )
         }

         composable(Screen.TryOn.route) {
             TryOnScreen(
                 onNavigateBack = { navController.popBackStack() }
             )
         }
     }
}

7.3 Update HomeScreen

Modificar: core/presentation/home/HomeScreen.kt
@Composable
fun HomeScreen(
onLogout: () -> Unit,
onNavigateToWardrobe: () -> Unit = {}
) {
Scaffold(
topBar = { /* ... */ },
floatingActionButton = {
FloatingActionButton(onClick = onNavigateToWardrobe) {
Icon(Icons.Default.Checkroom, "Mi Guardarropa")
}
}
) { padding ->
Column(
modifier = Modifier
.fillMaxSize()
.padding(padding),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.Center
) {
Button(onClick = onNavigateToWardrobe) {
Text("Ir a Mi Guardarropa")
}
}
}
}

 ---
Cronograma Estimado

| Fase   | Descripción                    | Complejidad        |
 |--------|--------------------------------|--------------------|
| Fase 1 | Database Schema (Supabase)     | Baja - 1 sesión    |
| Fase 2 | Wardrobe - Data Layer          | Media - 2 sesiones |
| Fase 3 | Wardrobe - Domain Layer        | Baja - 1 sesión    |
| Fase 4 | Wardrobe - Presentation        | Media - 2 sesiones |
| Fase 5 | Try-On - Data Layer (Gemini)   | Alta - 3 sesiones  |
| Fase 6 | Try-On - Domain & Presentation | Alta - 3 sesiones  |
| Fase 7 | DI & Navigation                | Baja - 1 sesión    |

Total: ~13 sesiones de trabajo

 ---
Archivos Críticos a Crear/Modificar

Nuevos directorios:

sharedUI/src/commonMain/kotlin/baccaro/vestite/app/features/
├── wardrobe/
│   ├── data/
│   ├── domain/
│   ├── presentation/
│   └── di/
└── tryon/
├── data/
├── domain/
├── presentation/
└── di/

Modificaciones:

- App.kt - Registrar nuevos módulos Koin
- AppNavigation.kt - Agregar rutas
- HomeScreen.kt - Botones de navegación
- local.properties - API key de Gemini
- sharedUI/build.gradle.kts - BuildConfig para Gemini
- gradle/libs.versions.toml - Ktor client (si no está)

Scripts SQL:

- database/schema.sql - Schema completo
- database/seed.sql - Categorías iniciales

 ---
Dependencias Adicionales Necesarias

[versions]
ktor = "3.3.3"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

Ya tienes Ktor 3.3.3 en el proyecto, solo necesitas agregar los módulos de content-negotiation si no están.

 ---
Riesgos y Consideraciones

1. Gemini API:
- Límites de cuota diarios
- Costo por request (modelo Pro es más caro)
- Tiempo de generación (puede ser lento)
- Filtros de seguridad pueden bloquear imágenes
2. Almacenamiento:
- Imágenes generadas ocupan espacio en Supabase Storage
- Plan gratuito tiene límites (1GB storage)
- Considerar compresión de imágenes
3. Performance:
- Descarga de imágenes de prendas para try-on
- Compresión/resize de imágenes antes de enviar a Gemini
- Cache de categorías y prendas
4. UX:
- Feedback visual durante generación (puede tardar 10-30s)
- Manejo de errores claro
- Onboarding para explicar cómo usar el try-on
