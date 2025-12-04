package baccaro.vestite.app.core.util

object Constants {
    // Deep link scheme para OAuth callbacks
    const val DEEP_LINK_SCHEME = "vestite"
    const val DEEP_LINK_HOST = "auth"
    const val DEEP_LINK_CALLBACK = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/callback"

    // Validation
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_USERNAME_LENGTH = 3
}
