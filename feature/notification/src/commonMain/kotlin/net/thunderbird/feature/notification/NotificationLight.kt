package net.thunderbird.feature.notification

enum class NotificationLight {
    Disabled,
    AccountColor,
    SystemDefaultColor,
    White,
    Red,
    Green,
    Blue,
    Yellow,
    Cyan,
    Magenta,
    ;

    fun toColor(accountColor: Int): Int? {
        return when (this) {
            Disabled -> null
            AccountColor -> accountColor.toArgb()
            SystemDefaultColor -> defaultColorInt
            White -> 0xFFFFFF.toArgb()
            Red -> 0xFF0000.toArgb()
            Green -> 0x00FF00.toArgb()
            Blue -> 0x0000FF.toArgb()
            Yellow -> 0xFFFF00.toArgb()
            Cyan -> 0x00FFFF.toArgb()
            Magenta -> 0xFF00FF.toArgb()
        }
    }

    internal fun Int.toArgb() = this or 0xFF000000L.toInt()
}

internal expect val NotificationLight.defaultColorInt: Int
