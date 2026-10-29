package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisScreen() }
    }
}

@Composable
fun JarvisScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("jarvis_memory", android.content.Context.MODE_PRIVATE)
    }

    val messages = remember {
        mutableStateListOf<String>().apply {
            val saved = prefs.getString("history", null)
            if (saved != null) {
                try {
                    val array = JSONArray(saved)
                    for (i in 0 until array.length()) add(array.getString(i))
                } catch (_: Exception) {}
            }
            if (isEmpty()) add("JARVIS : Bonjour. Tous les systèmes sont opérationnels.")
        }
    }

    var userText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("SYSTÈME LOCAL ACTIF") }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var aiUrl by remember { mutableStateOf(prefs.getString("ai_url", "") ?: "") }
    var aiKey by remember { mutableStateOf(prefs.getString("ai_key", "") ?: "") }
    var aiModel by remember { mutableStateOf(prefs.getString("ai_model", "gpt-4o-mini") ?: "gpt-4o-mini") }
    var showSettings by remember { mutableStateOf(false) }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context))
            SpeechRecognizer.createSpeechRecognizer(context)
        else null
    }

    fun saveHistory() {
        val array = JSONArray()
        messages.takeLast(40).forEach { array.put(it) }
        prefs.edit().putString("history", array.toString()).apply()
    }

    fun speak(answer: String) {
        tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    fun addReply(question: String, answer: String) {
        messages.add("VOUS : $question")
        messages.add("JARVIS : $answer")
        while (messages.size > 40) messages.removeAt(0)
        saveHistory()
        speak(answer)
        status = "SYSTÈME LOCAL ACTIF"
    }

    fun openApp(action: String): String {
        return try {
            val intent = when (action) {
                "settings" -> Intent(Settings.ACTION_SETTINGS)
                "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "camera" -> Intent("android.media.action.IMAGE_CAPTURE")
                "calculator" -> Intent(Intent.ACTION_MAIN)
                    .addCategory("android.intent.category.APP_CALCULATOR")
                "browser" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                else -> null
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "J'ouvre l'application demandée."
            } else {
                "Cette commande n'est pas disponible."
            }
        } catch (_: Exception) {
            "Je ne parviens pas à ouvrir cette fonction sur cet appareil."
        }
    }

    fun askOnlineAI(question: String) {
        val endpoint = aiUrl.trim()
        val key = aiKey.trim()
        val model = aiModel.trim().ifBlank { "gpt-4o-mini" }

        if (endpoint.isBlank() || key.isBlank()) {
            addReply(
                question,
                "Je fonctionne actuellement en mode autonome. Pour utiliser une IA en ligne, configure son adresse et sa clé dans les paramètres."
            )
            return
        }

        status = "CONNEXION À L'IA..."
        messages.add("VOUS : $question")
        messages.add("JARVIS : Je consulte mon intelligence en ligne...")
        saveHistory()

        Thread {
            var answer = "La connexion à l'IA a échoué. Je reste disponible en mode local."
            var connection: HttpURLConnection? = null

            try {
                val url = URL(endpoint)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $key")
                }

                val history = JSONArray()
                history.put(
                    JSONObject()
                        .put("role", "system")
                        .put(
                            "content",
                            "Tu es JARVIS, un assistant personnel francophone, précis, poli et concis. Tu réponds en français."
                        )
                )

                messages.takeLast(12).forEach { line ->
                    when {
                        line.startsWith("VOUS : ") ->
                            history.put(
                                JSONObject()
                                    .put("role", "user")
                                    .put("content", line.removePrefix("VOUS : "))
                            )

                        line.startsWith("JARVIS : ") ->
                            history.put(
                                JSONObject()
                                    .put("role", "assistant")
                                    .put("content", line.removePrefix("JARVIS : "))
                            )
                    }
                }

                history.put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", question)
                )

                val body = JSONObject()
                    .put("model", model)
                    .put("messages", history)

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299)
                    connection.inputStream else connection.errorStream

                val response = stream.bufferedReader().use { it.readText() }

                if (responseCode in 200..299) {
                    val json = JSONObject(response)
                    answer = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    answer = "Le service IA a répondu avec une erreur ($responseCode). Vérifie la configuration."
                }
            } catch (_: Exception) {
                answer = "Impossible de joindre le service IA. Vérifie ta connexion et l'adresse du service."
            } finally {
                connection?.disconnect()
            }

            val finalAnswer = answer
            (context as? ComponentActivity)?.runOnUiThread {
                messages.add("JARVIS : $finalAnswer")
                while (messages.size > 40) messages.removeAt(0)
                saveHistory()
                speak(finalAnswer)
                status = "SYSTÈME EN LIGNE"
            }
        }.start()
    }

    fun respond(raw: String) {
        val question = raw.trim()
        if (question.isBlank()) return

        val q = question.lowercase(Locale.FRENCH)

        // MÉMOIRE LOCALE
        val rememberPrefix = listOf(
            "retiens que ",
            "souviens-toi que ",
            "mémorise que ",
            "memorise que "
        )
        val memoryPrefix = rememberPrefix.firstOrNull { q.startsWith(it) }

        if (memoryPrefix != null) {
            val information = question.substring(memoryPrefix.length).trim()
            if (information.isNotBlank()) {
                val old = prefs.getString("facts", "") ?: ""
                val updated = (old.lines() + information)
                    .filter { it.isNotBlank() }
                    .takeLast(100)

                prefs.edit()
                    .putString("facts", updated.joinToString("\n"))
                    .apply()

                addReply(question, "Information enregistrée dans ma mémoire locale.")
            }
            return
        }

        if (q.contains("qu'as-tu retenu") ||
            q.contains("que sais-tu de moi") ||
            q.contains("ma mémoire") ||
            q.contains("ma memoire")
        ) {
            val facts = prefs.getString("facts", "") ?: ""
            addReply(
                question,
                if (facts.isBlank())
                    "Ma mémoire personnelle est encore vide. Tu peux me dire : retiens que..."
                else
                    "Voici les informations mémorisées : $facts"
            )
            return
        }

        if (q.contains("oublie ma mémoire") ||
            q.contains("efface ma mémoire") ||
            q.contains("efface ma memoire")
        ) {
            prefs.edit().remove("facts").apply()
            addReply(question, "Ma mémoire personnelle locale a été effacée.")
            return
        }

        // COMMANDES LOCALES
        val answer = when {
            q.contains("bonjour") || q == "salut" ->
                "Bonjour. Heureux de vous retrouver. Comment puis-je vous aider ?"

            q.contains("qui es-tu") || q.contains("qui es tu") ->
                "Je suis JARVIS, votre assistant personnel hybride."

            q.contains("ton nom") ->
                "Je suis JARVIS."

            q.contains("heure") ->
                "Il est actuellement " +
                    SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date()) + "."

            q.contains("date") || q.contains("quel jour") ->
                "Nous sommes le " +
                    SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(Date()) + "."

            q.contains("ouvre les paramètres") ||
                q.contains("ouvre les parametres") ->
                openApp("settings")

            q.contains("ouvre le wifi") ||
                q.contains("ouvre le wi-fi") ->
                openApp("wifi")

            q.contains("ouvre bluetooth") ->
                openApp("bluetooth")

            q.contains("ouvre la caméra") ||
                q.contains("ouvre la camera") ->
                openApp("camera")

            q.contains("ouvre la calculatrice") ->
                openApp("calculator")

            q.contains("ouvre internet") ||
                q.contains("ouvre le navigateur") ->
                openApp("browser")

            q.startsWith("cherche ") || q.startsWith("recherche ") -> {
                val terms = question.substringAfter(" ").trim()
                try {
                    val url = "https://www.google.com/search?q=" + Uri.encode(terms)
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    )
                    "Je lance la recherche sur Internet."
                } catch (_: Exception) {
                    "Je n'arrive pas à ouvrir le navigateur."
                }
            }

            q.contains("merci") ->
                "Avec plaisir. Je reste à votre disposition."

            q.contains("au revoir") ->
                "À bientôt. Je reste en veille."

            q.contains("mode autonome") ->
                "Le mode autonome utilise mes commandes intégrées et ma mémoire locale. Aucune IA en ligne n'est nécessaire."

            else -> null
        }

        if (answer != null) {
            addReply(question, answer)
        } else if (aiUrl.isNotBlank() && aiKey.isNotBlank()) {
            askOnlineAI(question)
        } else {
            addReply(
                question,
                "Je n'ai pas encore de réponse intégrée à cette demande. Je peux gérer mes commandes locales et ma mémoire. Configure une IA en ligne si tu souhaites des réponses plus avancées."
            )
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez à JARVIS")
            }
            recognizer?.startListening(intent)
        } else {
            status = "AUTORISATION MICRO REFUSÉE"
        }
    }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.let {
                    it.language = Locale.FRANCE
                    it.setPitch(0.78f)
                    it.setSpeechRate(0.88f)
                }
            }
        }

        tts = engine
        engine.language = Locale.FRANCE
        engine.setPitch(0.78f)
        engine.setSpeechRate(0.88f)

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                status = "JE VOUS ÉCOUTE..."
            }

            override fun onBeginningOfSpeech() {
                status = "ÉCOUTE EN COURS"
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                status = "ANALYSE..."
            }

            override fun onError(error: Int) {
                status = "ÉCOUTE TERMINÉE — RÉESSAYEZ"
            }

            override fun onResults(results: Bundle?) {
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                if (!spoken.isNullOrBlank()) {
                    respond(spoken)
                } else {
                    status = "AUCUN MESSAGE REÇU"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            recognizer?.destroy()
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF07111F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                "J.A.R.V.I.S",
                color = Color(0xFF55DFFF),
                fontSize = 32.sp
            )

            Text(
                "ASSISTANT PERSONNEL HYBRIDE",
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "◉",
                color = Color(0xFF55DFFF),
                fontSize = 66.sp
            )

            Text(
                status,
                color = Color(0xFF55DFFF),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showSettings = !showSettings }) {
                    Text(if (showSettings) "FERMER CONFIG" else "CONFIGURATION IA")
                }

                Button(onClick = {
                    val facts = prefs.getString("facts", "") ?: ""
                    addReply(
                        "État de ma mémoire",
                        if (facts.isBlank())
                            "Mémoire locale vide."
                        else
                            "Mémoire active : ${facts.lines().size} information(s) enregistrée(s)."
                    )
                }) {
                    Text("MÉMOIRE")
                }
            }

            if (showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF14283D),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "IA EN LIGNE — FACULTATIVE",
                        color = Color(0xFF55DFFF)
                    )

                    Text(
                        "Sans ces informations, JARVIS reste en mode autonome.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    BasicTextField(
                        value = aiUrl,
                        onValueChange = { aiUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFF20384F),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp
                        ),
                        decorationBox = { inner ->
                            Box {
                                if (aiUrl.isEmpty()) {
                                    Text(
                                        "Adresse API compatible OpenAI",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    BasicTextField(
                        value = aiModel,
                        onValueChange = { aiModel = it },
                        modifier = Modifier
                    
