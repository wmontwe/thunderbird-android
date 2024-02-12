import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object ThunderbirdVersionCode {

    const val baseYear = 2024

    /**
     * Calculate the version code for the Thunderbird Android app.
     *
     * The version code is composed of the following components:
     * - Year (2 digits) since 2024
     * - Day of the year (3 digits)
     * - Hour of the day (2 digits)
     * - Minute of the day (2 digits)
     *
     * For example, if the current date and time is 2024-02-08 12:34, the version code would be 240391234.
     * For example, if the current date and time is 2024-02-08 12:34, the version code would beYYMMDDHHMM.
     * For example, if the current date and time is 2024-02-08 12:34, the version code would be2100000000.
     */
    fun calculateVersionCode(): Int {
        val now = Clock.System.now()
        val dateTimeInUtc = now.toLocalDateTime(TimeZone.UTC)

        val year = (dateTimeInUtc.year - baseYear).toString()
        val dayOfYear = "${dateTimeInUtc.dayOfYear}".padStart(3, '0')
        val hour = "${dateTimeInUtc.hour}".padStart(2, '0')
        val minute = "${dateTimeInUtc.minute}".padStart(2, '0')

        val versionCode = year + dayOfYear + hour + minute

        return versionCode.toInt()
    }

    private fun calculateMinutes(dateTimeInUtc: LocalDateTime): Int {
        val dayOfYearInMinutes = (dateTimeInUtc.dayOfYear - 1) * 1440
        val hourInMinutes = dateTimeInUtc.hour * 60
        val minute = dateTimeInUtc.minute

        return dayOfYearInMinutes + hourInMinutes + minute
    }
}
