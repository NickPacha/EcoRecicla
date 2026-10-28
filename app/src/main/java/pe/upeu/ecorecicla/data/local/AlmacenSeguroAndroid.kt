package pe.upeu.ecorecicla.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AlmacenSeguroAndroid(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ecorecicla_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun guardarToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun obtenerToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun guardarUsuario(usuario: String) {
        sharedPreferences.edit().putString(KEY_USUARIO, usuario).apply()
    }

    fun obtenerUsuario(): String? {
        return sharedPreferences.getString(KEY_USUARIO, null)
    }

    fun guardarRol(rol: String) {
        sharedPreferences.edit().putString(KEY_ROL, rol).apply()
    }

    fun obtenerRol(): String? {
        return sharedPreferences.getString(KEY_ROL, null)
    }

    fun guardarPrivilegios(privilegios: Set<String>) {
        sharedPreferences.edit().putStringSet(KEY_PRIVILEGIOS, privilegios).apply()
    }

    fun obtenerPrivilegios(): Set<String> {
        return sharedPreferences.getStringSet(KEY_PRIVILEGIOS, emptySet()) ?: emptySet()
    }

    fun limpiar() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USUARIO = "usuario"
        private const val KEY_ROL = "rol"
        private const val KEY_PRIVILEGIOS = "privilegios"
    }
}
