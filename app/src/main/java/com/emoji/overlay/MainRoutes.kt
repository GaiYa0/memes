package com.emoji.overlay

internal const val TAG = "EmojiOverlay"

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val ALBUM_IMPORT = "album_import"
    const val BROWSE = "browse"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    const val CATEGORIES = "categories"
    const val CATEGORY_DETAIL = "category/{categoryId}"
    const val SEARCH = "search"

    fun categoryDetail(id: Long) = "category/$id"
}

internal fun isValidTopLevelRoute(route: String): Boolean {
    return route == Routes.IMPORT ||
        route == Routes.BROWSE ||
        route == Routes.FAVORITES ||
        route == Routes.RECENT ||
        route == Routes.CATEGORIES ||
        route == Routes.SEARCH
}
