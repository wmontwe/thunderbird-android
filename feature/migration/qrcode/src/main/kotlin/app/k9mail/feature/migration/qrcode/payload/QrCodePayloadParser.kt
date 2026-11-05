package app.k9mail.feature.migration.qrcode.payload

import com.squareup.moshi.JsonDataException
import java.io.IOException
import net.thunderbird.components.core.logging.Logger

private const val TAG = "QrCodePayloadParser"

internal class QrCodePayloadParser(
    private val qrCodePayloadAdapter: QrCodePayloadAdapter,
    private val logger: Logger,
) {
    /**
     * Parses the QR code payload as JSON and reads it into [QrCodeData].
     *
     * @return [QrCodeData] if the JSON was parsed successfully and has the correct structure, `null` otherwise.
     */
    fun parse(payload: String): QrCodeData? {
        return try {
            qrCodePayloadAdapter.fromJson(payload)
        } catch (e: JsonDataException) {
            logger.debug(tag = TAG, throwable = e) { "Failed to parse JSON" }
            null
        } catch (e: IOException) {
            logger.debug(tag = TAG, throwable = e) { "Unexpected IOException" }
            null
        }
    }
}
