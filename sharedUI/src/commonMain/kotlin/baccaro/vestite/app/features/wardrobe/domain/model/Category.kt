package baccaro.vestite.app.features.wardrobe.domain.model

data class Category(
    val id: String,
    val slug: CategorySlug,
    val displayName: String
)

enum class CategorySlug(val value: String) {
    UPPER("upper"),
    LOWER("lower"),
    FOOTWEAR("footwear");

    companion object {
        fun fromValue(value: String): CategorySlug {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown category slug: $value")
        }
    }
}
