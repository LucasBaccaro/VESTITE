package baccaro.vestite.app.features.authentication.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baccaro.vestite.app.features.authentication.domain.usecase.SignInUseCase
import baccaro.vestite.app.features.authentication.domain.usecase.SignInWithGoogleNativeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Login
 */
class LoginViewModel(
    private val signInUseCase: SignInUseCase,
    private val signInWithGoogleNativeUseCase: SignInWithGoogleNativeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun onPasswordVisibilityToggle() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSignIn() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            signInUseCase(
                email = _state.value.email,
                password = _state.value.password
            ).fold(
                onSuccess = { user ->
                    _state.update {
                        it.copy(isLoading = false, isSuccess = true)
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error desconocido"
                        )
                    }
                }
            )
        }
    }

    /**
     * Maneja el resultado del sign-in con Google obtenido por KMPAuth
     *
     * @param idToken Token de ID de Google. Null si hubo error o se canceló
     */
    fun onGoogleSignInResult(idToken: String?) {
        if (idToken == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = "Error al iniciar sesión con Google"
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            signInWithGoogleNativeUseCase(idToken).fold(
                onSuccess = { user ->
                    _state.update {
                        it.copy(isLoading = false, isSuccess = true)
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al iniciar sesión con Google"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
