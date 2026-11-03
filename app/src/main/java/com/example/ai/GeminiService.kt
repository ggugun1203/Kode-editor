package com.example.ai

import com.example.model.FileDiffProposal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API Key belum dimasukkan"))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val payload = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Ping. Respond with exactly: OK")
                            })
                        })
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 10)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success("Koneksi berhasil! Model: $model terhubung.")
            } else {
                val errorMsg = parseErrorMessage(bodyString)
                Result.failure(Exception("Gagal: HTTP ${response.code} - $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun askAi(
        prompt: String,
        apiKey: String,
        model: String,
        temperature: Float,
        projectContext: String,
        activeFileName: String?,
        activeFileContent: String?
    ): Result<AiResponseResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API Key belum dikonfigurasi. Silakan atur di menu Settings."))
        }

        try {
            val systemInstruction = """
You are DroidStudio AI, an expert Senior Mobile-First Full-Stack Engineer and Software Architect.
You help developers build, edit, refactor, and debug Web (HTML/CSS/JS/PHP/SQL) and Android applications directly on mobile smartphones.

CURRENT WORKSPACE CONTEXT:
$projectContext

${if (!activeFileName.isNullOrBlank() && activeFileContent != null) "CURRENT OPEN FILE ($activeFileName):\n```\n$activeFileContent\n```" else ""}

RULES FOR CODE GENERATION & FILE MODIFICATIONS:
When you suggest creating or modifying files, ALWAYS format each file change strictly using this pattern so the mobile app can parse and present the Diff & Approval preview to the user:

<<<FILE_START>>>
path: <relative_path_to_file>
summary: <1 sentence description of change>
<<<CODE_START>>>
<entire new file content here>
<<<CODE_END>>>
<<<FILE_END>>>

You may provide multiple file blocks if creating or modifying several files.
Before or after the file blocks, include your clear explanation in plain Indonesian or English as requested by the user.
""".trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemInstruction\n\nUSER REQUEST: $prompt")
                        })
                    })
                })
            }

            val payload = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", temperature)
                    put("maxOutputTokens", 8192)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val err = parseErrorMessage(bodyString)
                return@withContext Result.failure(Exception("Gemini API Error (${response.code}): $err"))
            }

            val json = JSONObject(bodyString)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text") ?: "Tidak ada respons dari AI."

            val parsedProposals = parseFileProposals(textOutput, activeFileName, activeFileContent)
            val cleanChatOutput = cleanOutputText(textOutput)

            Result.success(AiResponseResult(text = cleanChatOutput, rawText = textOutput, proposals = parsedProposals))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            obj.optJSONObject("error")?.optString("message") ?: jsonStr
        } catch (e: Exception) {
            jsonStr.take(150)
        }
    }

    private fun parseFileProposals(
        text: String,
        activeFileName: String?,
        activeFileContent: String?
    ): List<FileDiffProposal> {
        val proposals = mutableListOf<FileDiffProposal>()
        val fileRegex = Regex("""<<<FILE_START>>>\s*path:\s*(.*?)\s*summary:\s*(.*?)\s*<<<CODE_START>>>(.*?)<<<CODE_END>>>\s*<<<FILE_END>>>""", RegexOption.DOT_MATCHES_ALL)

        val matches = fileRegex.findAll(text)
        for (match in matches) {
            val path = match.groupValues[1].trim()
            val summary = match.groupValues[2].trim()
            val newCode = match.groupValues[3].trim()

            val originalCode = if (activeFileName != null && path.endsWith(activeFileName)) {
                activeFileContent ?: ""
            } else {
                ""
            }

            proposals.add(
                FileDiffProposal(
                    targetFilePath = path,
                    originalContent = originalCode,
                    proposedContent = newCode,
                    summary = summary
                )
            )
        }
        return proposals
    }

    private fun cleanOutputText(text: String): String {
        // Strip the machine file blocks for cleaner chat reading
        return text.replace(Regex("""<<<FILE_START>>>.*?<<<FILE_END>>>""", RegexOption.DOT_MATCHES_ALL), "*(Perubahan file telah disiapkan di panel persetujuan di bawah)*").trim()
    }
}

data class AiResponseResult(
    val text: String,
    val rawText: String,
    val proposals: List<FileDiffProposal>
)
