package baccaro.vestite.app.features.authentication.presentation.login

/**
 * Estado de la pantalla de Login
 */
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isPasswordVisible: Boolean = false
)
