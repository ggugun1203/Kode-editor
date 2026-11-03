package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SettingsManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("droidstudio_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "DroidStudioKeyAlias"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private const val PREF_GEMINI_KEY = "encrypted_gemini_api_key"
        private const val PREF_GEMINI_MODEL = "gemini_model"
        private const val PREF_GEMINI_TEMP = "gemini_temp"
        private const val PREF_GITHUB_TOKEN = "encrypted_github_token"
        private const val PREF_GITHUB_REPO = "github_repo"
        private const val PREF_GITHUB_BRANCH = "github_branch"
        private const val PREF_SERVER_PORT = "server_port"
        private const val PREF_AUTO_SAVE = "auto_save"
        private const val PREF_FONT_SIZE = "editor_font_size"
        private const val PREF_EDITOR_THEME = "editor_theme"
        private const val PREF_LAST_PROJECT_ID = "last_project_id"
    }

    private val _geminiApiKey = MutableStateFlow(getEncryptedString(PREF_GEMINI_KEY))
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _geminiModel = MutableStateFlow(prefs.getString(PREF_GEMINI_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash")
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

    private val _temperature = MutableStateFlow(prefs.getFloat(PREF_GEMINI_TEMP, 0.7f))
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _githubToken = MutableStateFlow(getEncryptedString(PREF_GITHUB_TOKEN))
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    private val _githubRepo = MutableStateFlow(prefs.getString(PREF_GITHUB_REPO, "") ?: "")
    val githubRepo: StateFlow<String> = _githubRepo.asStateFlow()

    private val _githubBranch = MutableStateFlow(prefs.getString(PREF_GITHUB_BRANCH, "main") ?: "main")
    val githubBranch: StateFlow<String> = _githubBranch.asStateFlow()

    private val _serverPort = MutableStateFlow(prefs.getInt(PREF_SERVER_PORT, 8080))
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _autoSave = MutableStateFlow(prefs.getBoolean(PREF_AUTO_SAVE, true))
    val autoSave: StateFlow<Boolean> = _autoSave.asStateFlow()

    private val _fontSize = MutableStateFlow(prefs.getInt(PREF_FONT_SIZE, 14))
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _editorTheme = MutableStateFlow(prefs.getString(PREF_EDITOR_THEME, "VS Code Dark+") ?: "VS Code Dark+")
    val editorTheme: StateFlow<String> = _editorTheme.asStateFlow()

    init {
        // If no user-set Gemini key exists, check BuildConfig
        if (_geminiApiKey.value.isEmpty()) {
            val buildConfigKey = try {
                BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey != "your_api_key_here") {
                saveGeminiApiKey(buildConfigKey)
            }
        }
    }

    fun saveGeminiApiKey(key: String) {
        saveEncryptedString(PREF_GEMINI_KEY, key.trim())
        _geminiApiKey.value = key.trim()
    }

    fun removeGeminiApiKey() {
        saveEncryptedString(PREF_GEMINI_KEY, "")
        _geminiApiKey.value = ""
    }

    fun setGeminiModel(model: String) {
        prefs.edit().putString(PREF_GEMINI_MODEL, model).apply()
        _geminiModel.value = model
    }

    fun setTemperature(temp: Float) {
        prefs.edit().putFloat(PREF_GEMINI_TEMP, temp).apply()
        _temperature.value = temp
    }

    fun saveGithubToken(token: String) {
        saveEncryptedString(PREF_GITHUB_TOKEN, token.trim())
        _githubToken.value = token.trim()
    }

    fun removeGithubToken() {
        saveEncryptedString(PREF_GITHUB_TOKEN, "")
        _githubToken.value = ""
    }

    fun setGithubRepo(repo: String) {
        prefs.edit().putString(PREF_GITHUB_REPO, repo.trim()).apply()
        _githubRepo.value = repo.trim()
    }

    fun setGithubBranch(branch: String) {
        prefs.edit().putString(PREF_GITHUB_BRANCH, branch.trim()).apply()
        _githubBranch.value = branch.trim()
    }

    fun setServerPort(port: Int) {
        prefs.edit().putInt(PREF_SERVER_PORT, port).apply()
        _serverPort.value = port
    }

    fun setAutoSave(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_SAVE, enabled).apply()
        _autoSave.value = enabled
    }

    fun setFontSize(size: Int) {
        prefs.edit().putInt(PREF_FONT_SIZE, size).apply()
        _fontSize.value = size
    }

    fun setEditorTheme(theme: String) {
        prefs.edit().putString(PREF_EDITOR_THEME, theme).apply()
        _editorTheme.value = theme
    }

    fun getLastProjectId(): String? = prefs.getString(PREF_LAST_PROJECT_ID, null)

    fun setLastProjectId(id: String) {
        prefs.edit().putString(PREF_LAST_PROJECT_ID, id).apply()
    }

    // --- Android KeyStore AES-GCM Encrypted Storage ---
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun saveEncryptedString(prefKey: String, plainText: String) {
        if (plainText.isEmpty()) {
            prefs.edit().remove(prefKey).apply()
            return
        }
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString(prefKey, encoded).apply()
        } catch (e: Exception) {
            // Fallback obfuscated in case KeyStore is unavailable on test environment
            val fallback = Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            prefs.edit().putString(prefKey, "RAW:$fallback").apply()
        }
    }

    private fun getEncryptedString(prefKey: String): String {
        val encoded = prefs.getString(prefKey, null) ?: return ""
        if (encoded.startsWith("RAW:")) {
            val raw = encoded.substring(4)
            return String(Base64.decode(raw, Base64.NO_WRAP), Charsets.UTF_8)
        }
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
