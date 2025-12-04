# Supabase-KT: Kotlin Multiplatform Client

Supabase-KT is a Kotlin Multiplatform client library for Supabase, providing comprehensive integration with Supabase's backend-as-a-service platform. It enables developers to interact with Supabase services including authentication, database (PostgREST), real-time subscriptions, storage, and edge functions across multiple Kotlin targets including JVM, Android, iOS, JavaScript, and WASM. The library is built on top of Ktor's HTTP client and follows a plugin-based architecture that allows developers to install only the modules they need.

The library offers type-safe APIs with Kotlin-idiomatic constructs such as coroutines, flows, and DSL builders. It supports advanced features like automatic session management with token refresh, real-time database change subscriptions via WebSockets, resumable file uploads, multi-factor authentication, and OAuth provider integration. The modular design ensures minimal dependencies while providing seamless integration between different Supabase services through a unified client interface.

## Creating a Supabase Client

Initialize the Supabase client with required plugins

```kotlin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.logging.LogLevel

val supabase = createSupabaseClient(
    supabaseUrl = "https://xyzcompany.supabase.co",
    supabaseKey = "your-anon-key"
) {
    // Set logging level
    defaultLogLevel = LogLevel.DEBUG

    // Install desired plugins
    install(Auth) {
        // Configure authentication
        flowType = FlowType.PKCE
        alwaysAutoRefresh = true
    }
    install(Postgrest) {
        // Configure database client
        defaultSchema = "public"
        propertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
    }
    install(Storage)
    install(Realtime)
    install(Functions)
}
```

## Email Authentication Sign Up

Register new users with email and password

```kotlin
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException

try {
    val result = supabase.auth.signUpWith(Email) {
        email = "user@example.com"
        password = "securePassword123"
        data = buildJsonObject {
            put("username", "johndoe")
            put("age", 25)
        }
    }

    if (result != null) {
        println("User created with ID: ${result.id}")
        println("Email confirmation required")
    } else {
        println("User logged in successfully (auto-confirm enabled)")
    }
} catch (e: AuthWeakPasswordException) {
    println("Password too weak: ${e.reasons}")
} catch (e: AuthRestException) {
    println("Auth error: ${e.error}")
} catch (e: RestException) {
    println("Network error: ${e.message}")
}
```

## Email Authentication Sign In

Authenticate existing users

```kotlin
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus

try {
    supabase.auth.signInWith(Email) {
        email = "user@example.com"
        password = "securePassword123"
    }

    // Access the current session
    val session = supabase.auth.currentSessionOrNull()
    if (session != null) {
        println("Access Token: ${session.accessToken}")
        println("User ID: ${session.user?.id}")
        println("Email: ${session.user?.email}")
    }

    // Monitor session status with Flow
    supabase.auth.sessionStatus.collect { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                println("User authenticated: ${status.session.user?.email}")
            }
            is SessionStatus.NotAuthenticated -> {
                println("User not authenticated")
            }
            is SessionStatus.LoadingFromStorage -> {
                println("Loading session from storage")
            }
            is SessionStatus.RefreshError -> {
                println("Session refresh failed: ${status.cause}")
            }
        }
    }
} catch (e: AuthRestException) {
    println("Invalid credentials: ${e.error}")
}
```

## OAuth Authentication (Google)

Sign in with OAuth providers

```kotlin
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google

try {
    // Opens browser for OAuth flow
    supabase.auth.signInWith(Google) {
        // Optional: Specify custom redirect URL
        // redirectUrl = "myapp://callback"

        // Optional: Request specific scopes
        scopes = listOf("email", "profile")

        // Optional: Disable automatic URL opening
        automaticallyOpenUrl = false
    }

    // Handle OAuth callback in your deep link handler
    // The session will be automatically established
} catch (e: AuthRestException) {
    println("OAuth error: ${e.error}")
}
```

## Anonymous Sign In

Create anonymous user sessions

```kotlin
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

try {
    supabase.auth.signInAnonymously(
        data = buildJsonObject {
            put("device_id", "unique-device-identifier")
        }
    )

    val user = supabase.auth.currentUserOrNull()
    println("Anonymous user ID: ${user?.id}")

    // Later upgrade anonymous user to permanent account
    supabase.auth.updateUser {
        email = "user@example.com"
        password = "newPassword123"
    }
} catch (e: AuthRestException) {
    println("Anonymous sign in failed: ${e.error}")
}
```

## Multi-Factor Authentication

Enable and verify TOTP-based MFA

```kotlin
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa.FactorType

// Sign in first
supabase.auth.signInWith(Email) {
    email = "user@example.com"
    password = "password123"
}

// Enroll in MFA
val enrollResult = supabase.auth.mfa.enroll(FactorType.TOTP) {
    friendlyName = "My Authenticator"
}

println("Scan this QR code: ${enrollResult.totp.qrCode}")
println("Or enter this secret: ${enrollResult.totp.secret}")

// User scans QR code with authenticator app
// Verify the enrollment with generated code
val challengeId = supabase.auth.mfa.challenge(enrollResult.id)
supabase.auth.mfa.verify(
    factorId = enrollResult.id,
    challengeId = challengeId,
    code = "123456" // Code from authenticator app
)

// On subsequent logins, verify MFA
val factors = supabase.auth.mfa.listFactors()
val factor = factors.totp.first()
val challenge = supabase.auth.mfa.challenge(factor.id)
supabase.auth.mfa.verify(factor.id, challenge, "654321")
```

## Update User Profile

Modify authenticated user information

```kotlin
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserUpdateBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

try {
    val updatedUser = supabase.auth.updateUser {
        email = "newemail@example.com"
        password = "newSecurePassword456"

        data = buildJsonObject {
            put("username", "newusername")
            put("avatar_url", "https://example.com/avatar.jpg")
        }
    }

    println("User updated: ${updatedUser.email}")
} catch (e: AuthRestException) {
    println("Update failed: ${e.error}")
}
```

## Password Reset

Send password reset email

```kotlin
import io.github.jan.supabase.auth.auth

try {
    supabase.auth.resetPasswordForEmail(
        email = "user@example.com",
        redirectUrl = "myapp://reset-password"
    )
    println("Password reset email sent")
} catch (e: AuthRestException) {
    println("Failed to send reset email: ${e.error}")
}
```

## OTP Verification

Verify email or phone OTP codes

```kotlin
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType

// Verify email OTP
try {
    supabase.auth.verifyEmailOtp(
        type = OtpType.Email.EMAIL,
        email = "user@example.com",
        token = "123456"
    )
    println("Email verified successfully")
} catch (e: AuthRestException) {
    println("Invalid OTP: ${e.error}")
}

// Verify phone OTP
try {
    supabase.auth.verifyPhoneOtp(
        type = OtpType.Phone.SMS,
        phone = "+1234567890",
        token = "654321"
    )
    println("Phone verified successfully")
} catch (e: AuthRestException) {
    println("Invalid OTP: ${e.error}")
}
```

## Sign Out

End user session

```kotlin
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.SignOutScope

try {
    // Sign out locally only
    supabase.auth.signOut(scope = SignOutScope.LOCAL)

    // Or sign out from all devices
    supabase.auth.signOut(scope = SignOutScope.GLOBAL)

    println("User signed out")
} catch (e: RestException) {
    println("Sign out error: ${e.message}")
}
```

## Select Database Records

Query data from PostgREST tables

```kotlin
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val categoryId: Int
)

try {
    // Select all products
    val allProducts = supabase.from("products")
        .select()
        .decodeList<Product>()

    // Select with filters
    val filteredProducts = supabase.from("products")
        .select {
            filter {
                Product::price gt 100.0
                Product::categoryId eq 5
            }
        }
        .decodeList<Product>()

    // Select single record
    val singleProduct = supabase.from("products")
        .select {
            filter {
                Product::id eq 42
            }
        }
        .decodeSingle<Product>()

    // Select specific columns
    val partialData = supabase.from("products")
        .select(columns = Columns.list("id", "name"))
        .decodeList<Product>()

    // Select with joins (related data)
    @Serializable
    data class ProductWithCategory(
        val id: Int,
        val name: String,
        val category: Category
    )

    @Serializable
    data class Category(val id: Int, val name: String)

    val productsWithCategories = supabase.from("products")
        .select(columns = Columns.raw("id, name, category:categories(id, name)"))
        .decodeList<ProductWithCategory>()

    println("Found ${allProducts.size} products")
} catch (e: PostgrestRestException) {
    println("Database error: ${e.error}")
}
```

## Insert Database Records

Add new records to tables

```kotlin
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class Product(val id: Int, val name: String, val price: Double)

try {
    // Insert single record
    val newProduct = supabase.from("products")
        .insert(buildJsonObject {
            put("name", "New Product")
            put("price", 29.99)
            put("category_id", 1)
        }) {
            select()
        }
        .decodeSingle<Product>()

    println("Created product with ID: ${newProduct.id}")

    // Insert multiple records
    val products = supabase.from("products")
        .insert(listOf(
            buildJsonObject {
                put("name", "Product 1")
                put("price", 19.99)
            },
            buildJsonObject {
                put("name", "Product 2")
                put("price", 39.99)
            }
        )) {
            select()
        }
        .decodeList<Product>()

    println("Created ${products.size} products")
} catch (e: PostgrestRestException) {
    println("Insert failed: ${e.error}")
}
```

## Update Database Records

Modify existing records

```kotlin
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

try {
    val updatedProducts = supabase.from("products")
        .update(buildJsonObject {
            put("price", 24.99)
            put("on_sale", true)
        }) {
            filter {
                Product::categoryId eq 5
            }
            select()
        }
        .decodeList<Product>()

    println("Updated ${updatedProducts.size} products")
} catch (e: PostgrestRestException) {
    println("Update failed: ${e.error}")
}
```

## Upsert Database Records

Insert or update records based on conflict

```kotlin
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.request.UpsertRequestBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

try {
    val product = supabase.from("products")
        .upsert(buildJsonObject {
            put("id", 100)
            put("name", "Updated Product")
            put("price", 49.99)
        }) {
            // Specify which columns trigger the conflict
            onConflict = "id"
            select()
        }
        .decodeSingle<Product>()

    println("Upserted product: ${product.name}")
} catch (e: PostgrestRestException) {
    println("Upsert failed: ${e.error}")
}
```

## Delete Database Records

Remove records from tables

```kotlin
import io.github.jan.supabase.postgrest.from

try {
    // Delete with filter
    supabase.from("products")
        .delete {
            filter {
                Product::id eq 42
            }
        }

    println("Product deleted")

    // Delete multiple records
    supabase.from("products")
        .delete {
            filter {
                Product::price lt 10.0
            }
        }

    println("Deleted cheap products")
} catch (e: PostgrestRestException) {
    println("Delete failed: ${e.error}")
}
```

## Call RPC Functions

Execute database functions

```kotlin
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class SearchResult(val id: Int, val name: String, val rank: Double)

try {
    // Call function without parameters
    val result = supabase.postgrest.rpc("get_total_sales")
        .decodeSingle<Double>()

    println("Total sales: $result")

    // Call function with parameters
    val searchResults = supabase.postgrest.rpc(
        function = "search_products",
        parameters = buildJsonObject {
            put("search_term", "laptop")
            put("min_price", 500)
            put("max_price", 2000)
        }
    ) {
        // Apply filters to the result
        filter {
            SearchResult::rank gt 0.5
        }
    }.decodeList<SearchResult>()

    println("Found ${searchResults.size} matching products")
} catch (e: PostgrestRestException) {
    println("RPC call failed: ${e.error}")
}
```

## Advanced Query Filters

Use complex filtering conditions

```kotlin
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.TextSearchType

try {
    val products = supabase.from("products")
        .select {
            filter {
                // Equality
                Product::categoryId eq 5

                // Not equal
                Product::categoryId neq 3

                // Greater than / less than
                Product::price gt 50.0
                Product::price gte 50.0
                Product::price lt 200.0
                Product::price lte 200.0

                // IN clause
                Product::categoryId isIn listOf(1, 2, 3)

                // LIKE pattern
                Product::name ilike "%laptop%"

                // NULL checks
                Product::description isNull true
                Product::description isNull false

                // Range checks
                Product::price range 50.0..200.0

                // Full text search
                Product::description textSearch "gaming laptop" to TextSearchType.PLAIN

                // Array contains
                or {
                    Product::tags contains listOf("electronics")
                    Product::featured eq true
                }
            }

            // Ordering
            order(column = "price", ascending = false)
            order(column = "name", ascending = true)

            // Pagination
            limit(10)
            range(0, 19)
        }
        .decodeList<Product>()

    println("Query returned ${products.size} products")
} catch (e: PostgrestRestException) {
    println("Query failed: ${e.error}")
}
```

## Create Storage Bucket

Set up file storage containers

```kotlin
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.BucketApi
import kotlin.time.Duration.Companion.hours

try {
    // Create public bucket
    supabase.storage.createBucket("avatars") {
        public = true
        fileSizeLimit = 5_000_000 // 5MB
        allowedMimeTypes = listOf("image/png", "image/jpeg", "image/gif")
    }

    // Create private bucket
    supabase.storage.createBucket("documents") {
        public = false
        fileSizeLimit = 50_000_000 // 50MB
    }

    println("Buckets created successfully")
} catch (e: RestException) {
    println("Bucket creation failed: ${e.message}")
}
```

## Upload Files to Storage

Store files in buckets

```kotlin
import io.github.jan.supabase.storage.storage
import io.ktor.utils.io.core.toByteArray

try {
    val bucket = supabase.storage["avatars"]

    // Upload from byte array
    val imageBytes = "image data".toByteArray()
    bucket.upload("user/profile.jpg", imageBytes) {
        upsert = false // Fail if file exists
        contentType = "image/jpeg"
    }

    // Upload with upsert (overwrite)
    bucket.upload("user/profile.jpg", imageBytes) {
        upsert = true
    }

    println("File uploaded successfully")

    // Get public URL for public bucket
    val publicUrl = bucket.publicUrl("user/profile.jpg")
    println("Public URL: $publicUrl")

    // Create authenticated URL for private bucket
    val privateBucket = supabase.storage["documents"]
    val signedUrl = privateBucket.createSignedUrl("user/document.pdf", 1.hours)
    println("Signed URL (valid 1 hour): $signedUrl")
} catch (e: RestException) {
    println("Upload failed: ${e.message}")
}
```

## Download Files from Storage

Retrieve stored files

```kotlin
import io.github.jan.supabase.storage.storage

try {
    val bucket = supabase.storage["avatars"]

    // Download as byte array
    val fileBytes = bucket.downloadAuthenticated("user/profile.jpg")
    println("Downloaded ${fileBytes.size} bytes")

    // Download public file (no auth required)
    val publicBytes = bucket.downloadPublic("public/logo.png")

    // List files in bucket
    val files = bucket.list("user/")
    files.forEach { file ->
        println("File: ${file.name}, Size: ${file.metadata?.size} bytes")
    }
} catch (e: RestException) {
    println("Download failed: ${e.message}")
}
```

## Resumable File Upload

Upload large files with resume capability

```kotlin
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.resumable.ResumableUploadState
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

try {
    val bucket = supabase.storage["videos"]
    val largeFileBytes = ByteArray(100_000_000) // 100MB

    // Upload with progress tracking
    bucket.resumable.upload(
        path = "uploads/large-video.mp4",
        dataProducer = { offset ->
            ByteReadChannel(largeFileBytes.copyOfRange(offset.toInt(), largeFileBytes.size))
        }
    ) {
        upsert = false

        // Track upload progress
        stateFlow.onEach { state ->
            when (state) {
                is ResumableUploadState.Idle -> println("Upload idle")
                is ResumableUploadState.Uploading -> {
                    val progress = (state.uploadedBytes.toDouble() / state.totalBytes) * 100
                    println("Uploading: ${progress.toInt()}%")
                }
                is ResumableUploadState.Finished -> println("Upload complete!")
                is ResumableUploadState.Error -> println("Upload error: ${state.message}")
            }
        }.collect()
    }
} catch (e: RestException) {
    println("Resumable upload failed: ${e.message}")
}
```

## Delete Storage Files

Remove files from buckets

```kotlin
import io.github.jan.supabase.storage.storage

try {
    val bucket = supabase.storage["avatars"]

    // Delete single file
    bucket.delete("user/old-profile.jpg")

    // Delete multiple files
    bucket.delete(listOf(
        "user/temp1.jpg",
        "user/temp2.jpg",
        "user/temp3.jpg"
    ))

    println("Files deleted successfully")
} catch (e: RestException) {
    println("Delete failed: ${e.message}")
}
```

## Real-time Channel Subscription

Listen to database changes via WebSockets

```kotlin
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Create channel
val channel = supabase.channel("products-changes")

// Listen for all changes
val allChangesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
    table = "products"
}

allChangesFlow.onEach { action ->
    when (action) {
        is PostgresAction.Insert -> {
            val product = action.decodeRecord<Product>()
            println("New product: ${product.name}")
        }
        is PostgresAction.Update -> {
            val product = action.decodeRecord<Product>()
            println("Updated product: ${product.name}")
        }
        is PostgresAction.Delete -> {
            val oldProduct = action.decodeOldRecord<Product>()
            println("Deleted product: ${oldProduct.name}")
        }
        is PostgresAction.Select -> {
            val product = action.decodeRecord<Product>()
            println("Selected product: ${product.name}")
        }
    }
}.launchIn(coroutineScope)

// Subscribe to start receiving events
channel.subscribe(blockUntilSubscribed = true)
```

## Real-time Broadcast Messages

Send and receive custom messages between clients

```kotlin
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.broadcast
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Serializable
data class ChatMessage(
    val userId: String,
    val message: String,
    val timestamp: Long
)

val channel = supabase.channel("chat-room")

// Listen for messages
val messageFlow = channel.broadcastFlow<ChatMessage>("message")
messageFlow.onEach { chatMessage ->
    println("[${chatMessage.userId}]: ${chatMessage.message}")
}.launchIn(coroutineScope)

// Subscribe to channel
channel.subscribe()

// Send message to all clients
channel.broadcast(
    event = "message",
    message = ChatMessage(
        userId = "user123",
        message = "Hello everyone!",
        timestamp = System.currentTimeMillis()
    )
)
```

## Real-time Presence Tracking

Track online users in channels

```kotlin
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceChangeFlow
import io.github.jan.supabase.realtime.track
import io.github.jan.supabase.realtime.untrack
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Serializable
data class UserPresence(
    val userId: String,
    val username: String,
    val status: String,
    val lastSeen: Long
)

val channel = supabase.channel("online-users")

// Track presence changes
val presenceFlow = channel.presenceChangeFlow()
presenceFlow.onEach { action ->
    val joins = action.decodeJoinsAs<UserPresence>()
    val leaves = action.decodeLeavesAs<UserPresence>()

    joins.forEach { (key, user) ->
        println("${user.username} joined")
    }

    leaves.forEach { (key, user) ->
        println("${user.username} left")
    }
}.launchIn(coroutineScope)

// Subscribe
channel.subscribe()

// Track own presence
channel.track(UserPresence(
    userId = "user123",
    username = "John Doe",
    status = "online",
    lastSeen = System.currentTimeMillis()
))

// Later update presence
channel.track(UserPresence(
    userId = "user123",
    username = "John Doe",
    status = "away",
    lastSeen = System.currentTimeMillis()
))

// Untrack when leaving
channel.untrack()
```

## Invoke Edge Functions

Call serverless edge functions

```kotlin
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FunctionRequest(val name: String, val age: Int)

@Serializable
data class FunctionResponse(val message: String, val data: String)

try {
    // Invoke function without body
    val response1 = supabase.functions("hello-world")
    println("Response: ${response1.bodyAsText()}")

    // Invoke with JSON body
    val response2 = supabase.functions(
        function = "process-data",
        body = FunctionRequest(name = "John", age = 30),
        headers = Headers.build {
            append(HttpHeaders.ContentType, "application/json")
        }
    )

    val result = Json.decodeFromString<FunctionResponse>(response2.bodyAsText())
    println("Message: ${result.message}")

    // Build reusable edge function
    val processFunction = supabase.functions.buildEdgeFunction(
        function = "process-data",
        region = FunctionRegion.US_WEST_1
    )

    val response3 = processFunction(FunctionRequest(name = "Jane", age = 25)) {
        append(HttpHeaders.ContentType, "application/json")
    }

    println("Response status: ${response3.status}")
} catch (e: RestException) {
    println("Function invocation failed: ${e.message}")
}
```

## Session Management and Persistence

Handle authentication session lifecycle

```kotlin
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

// Load session from storage on app start
val hasSession = supabase.auth.loadFromStorage(autoRefresh = true)
if (hasSession) {
    println("Session restored from storage")
}

// Wait for initialization to complete
supabase.auth.awaitInitialization()

// Monitor session status
supabase.auth.sessionStatus.collect { status ->
    when (status) {
        is SessionStatus.Authenticated -> {
            println("User authenticated")
            println("Access token expires at: ${status.session.expiresAt}")
        }
        is SessionStatus.NotAuthenticated -> {
            println("No active session")
        }
        is SessionStatus.LoadingFromStorage -> {
            println("Loading session from storage")
        }
        is SessionStatus.RefreshError -> {
            println("Token refresh failed: ${status.cause}")
            // Handle refresh error (e.g., prompt re-login)
        }
    }
}

// Manually refresh session
try {
    supabase.auth.refreshCurrentSession()
    println("Session refreshed")
} catch (e: RestException) {
    println("Refresh failed: ${e.message}")
}

// Import external session
val externalSession = UserSession(
    accessToken = "jwt-token",
    refreshToken = "refresh-token",
    expiresIn = 3600,
    tokenType = "bearer",
    user = null
)
supabase.auth.importSession(externalSession, autoRefresh = true)
```

## Supabase-KT is designed for building modern Kotlin applications that require backend services across mobile, web, and desktop platforms. The primary use cases include building real-time chat applications with presence tracking, e-commerce platforms with product catalogs and order management, content management systems with file storage, collaborative tools with live updates, and mobile apps requiring authentication and cloud sync. The library's multiplatform nature makes it ideal for teams building cross-platform applications with a shared Kotlin codebase while maintaining platform-specific optimizations.

## Integration follows a straightforward pattern: initialize the client with required plugins, configure platform-specific settings (like deep linking for OAuth on mobile), and use the type-safe APIs with coroutines and flows for asynchronous operations. The library handles complex scenarios like automatic token refresh, WebSocket reconnection, session persistence across app restarts, and resumable uploads transparently. It integrates seamlessly with dependency injection frameworks like Koin, works with serialization libraries (kotlinx.serialization by default, with Jackson/Moshi support), and can be customized with custom HTTP engines, serializers, and storage implementations. Error handling uses typed exceptions that make it easy to distinguish between authentication, network, and validation errors.