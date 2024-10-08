package com.fsck.k9.preferences

import com.fsck.k9.preferences.ServerSettingsDescriptions.AUTHENTICATION_TYPE
import com.fsck.k9.preferences.ServerSettingsDescriptions.CLIENT_CERTIFICATE_ALIAS
import com.fsck.k9.preferences.ServerSettingsDescriptions.CONNECTION_SECURITY
import com.fsck.k9.preferences.ServerSettingsDescriptions.HOST
import com.fsck.k9.preferences.ServerSettingsDescriptions.PASSWORD
import com.fsck.k9.preferences.ServerSettingsDescriptions.PORT
import com.fsck.k9.preferences.ServerSettingsDescriptions.USERNAME
import com.fsck.k9.preferences.ServerTypeConverter.toServerSettingsType
import com.fsck.k9.preferences.Settings.InvalidSettingValueException

internal class ServerSettingsValidator(
    private val settingsDescriptions: SettingsDescriptions = ServerSettingsDescriptions.SETTINGS,
) {
    fun validate(contentVersion: Int, server: SettingsFile.Server): ValidatedSettings.Server {
        val settings = convertServerSettingsToMap(server)

        val validatedSettings = Settings.validate(contentVersion, settingsDescriptions, settings, true)

        if (validatedSettings[AUTHENTICATION_TYPE] !is String) {
            throw InvalidSettingValueException("Missing '$AUTHENTICATION_TYPE' value")
        }

        if (validatedSettings[CONNECTION_SECURITY] !is String) {
            throw InvalidSettingValueException("Missing '$CONNECTION_SECURITY' value")
        }

        return ValidatedSettings.Server(
            type = toServerSettingsType(server.type!!),
            settings = validatedSettings,
            extras = server.extras.orEmpty(),
        )
    }

    private fun convertServerSettingsToMap(server: SettingsFile.Server): SettingsMap {
        return buildMap {
            server.host?.let { host -> put(HOST, host) }
            server.port?.let { port -> put(PORT, port) }
            server.connectionSecurity?.let { connectionSecurity -> put(CONNECTION_SECURITY, connectionSecurity) }
            server.authenticationType?.let { authenticationType -> put(AUTHENTICATION_TYPE, authenticationType) }
            server.username?.let { username -> put(USERNAME, username) }
            server.password?.let { password -> put(PASSWORD, password) }
            server.clientCertificateAlias?.let { clientCertificateAlias ->
                put(CLIENT_CERTIFICATE_ALIAS, clientCertificateAlias)
            }
        }
    }
}
