
package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

private val Background = Color(0xFF07111F)
private val Panel = Color(0xFF102238)
private val Accent = Color(0xFF22D3EE)
private val MainText = Color(0xFFEAF7FF)
private val MutedText = Color(0xFF9BB2C8)

private const val PREFS_NAME = "jarvis_memory"
private const val HISTORY_KEY = "conversation"
private const val ENDPOINT_KEY = "endpoint"
private const val MODEL_KEY = "model"
private const val API_KEY = "api_key"

data class ChatMessage(
    val text: String,
    val fromUser: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisScreen()
        }
    }
}

@Composable
fun JarvisScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, 0)
    }

    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("SYSTÈME EN LIGNE") }
    var thinking by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var endpoint by remember {
        mutableStateOf(
            prefs.getString(
                ENDPOINT_KEY,
                "https://api.openai.com/v1/chat/completions"
            ) ?: ""
        )
    }

    var model by remember {
        mutableStateOf(
            prefs.getString(MODEL_KEY, "gpt-4o-mini") ?: ""
        )
    }

    var apiKey by remember {
        mutableStateOf(prefs.getString(API_KEY, "") ?: "")
    }

    val history = remember {
        mutableStateListOf<ChatMessage>().apply {
            val saved = prefs.getString(HISTORY_KEY, null)

            if (!saved.isNullOrBlank()) {
                runCatching {
                    val array = JSONArray(saved)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        add(
                            ChatMessage(
                                text = item.optString("text"),
                                fromUser = item.optBoolean("user")
                            )
                        )
                    }
                }
            }

            if (isEmpty()) {
                add(
                    ChatMessage(
                        "Bonjour ! Je suis JARVIS, ton assistant personnel. Que puis-je faire pour toi ?",
                        false
                    )
                )
            }
        }
    }

    val listState = rememberLazyListState()

    var tts by remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun speak(message: String) {
        tts?.speak(
            message.take(3000),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_reply"
        )
    }

    fun saveMemory() {
        runCatching {
            val array = JSONArray()
            history.takeLast(100).forEach { message ->
                array.put(
                    JSONObject()
                        .put("text", message.text)
                        .put("user", message.fromUser)
                )
            }

            prefs.edit()
                .putString(HISTORY_KEY, array.toString())
                .apply()
        }
    }

    fun reply(message: String) {
        history.add(ChatMessage(message, false))
        saveMemory()
        status = "JARVIS PRÊT"
        speak(message)
    }

    fun openWeb(address: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(address))
            )
        }.onFailure {
            reply("Je n'arrive pas à ouvrir cette page.")
        }
    }

    fun openSystemSettings(action: String, message: String) {
        runCatching {
            context.startActivity(Intent(action))
            reply(message)
        }.onFailure {
            reply("Impossible d'ouvrir ce réglage.")
        }
    }

    fun askOnlineAI(question: String) {
        if (apiKey.isBlank()) {
            reply(
                "Pour utiliser l'intelligence artificielle en ligne, configure une clé API dans mes paramètres. Mes commandes intégrées restent disponibles."
            )
            return
        }

        if (endpoint.isBlank() || model.isBlank()) {
            reply("Configure l'adresse du service et le modèle IA dans les paramètres.")
            return
        }

        thinking = true
        status = "INTELLIGENCE ARTIFICIELLE..."

        val conversation = history.takeLast(12).toList()
        val savedEndpoint = endpoint
        val savedModel = model
        val savedKey = apiKey

        Thread {
            val answer: String = try {
                val connection =
                    URL(savedEndpoint).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 45000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $savedKey"
                )

                val messages = JSONArray()

                messages.put(
                    JSONObject()
                        .put("role", "system")
                        .put(
                            "content",
                            "Tu es JARVIS, un assistant personnel Android. " +
                                "Tu réponds toujours en français. " +
                                "Tu es poli, intelligent, direct et utile. " +
                                "Tu ne prétends jamais avoir effectué une action " +
                                "que tu n'as pas réellement exécutée."
                        )
                )

                conversation.forEach { item ->
                    messages.put(
                        JSONObject()
                            .put(
                                "role",
                                if (item.fromUser) "user" else "assistant"
                            )
                            .put("content", item.text)
                    )
                }

                messages.put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", question)
                )

                val body = JSONObject()
                    .put("model", savedModel)
                    .put("messages", messages)
                    .put("temperature", 0.7)

                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream.bufferedReader().use {
                    it.readText()
                }

                val result = if (code in 200..299) {
                    JSONObject(response)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    "Erreur du service IA : HTTP $code."
                }

                connection.disconnect()
                result
            } catch (e: Exception) {
                "Connexion impossible. Vérifie Internet et la configuration IA."
            }

            mainHandler.post {
                thinking = false
                reply(answer)
            }
        }.start()
    }

    fun executeCommand(rawText: String) {
        val question = rawText.trim()
        if (question.isEmpty() || thinking) return

        history.add(ChatMessage(question, true))
        input = ""
        saveMemory()

        val q = question.lowercase(Locale.FRENCH)

        when {
            q.contains("bonjour") ||
                q.contains("bonsoir") ||
                q == "salut" -> {
                reply("Bonjour ! Je suis à ton écoute.")
            }

            q.contains("qui es-tu") ||
                q.contains("qui est jarvis") -> {
                reply(
                    "Je suis JARVIS, ton assistant personnel Android. " +
                        "Je peux discuter, écouter tes commandes vocales " +
                        "et effectuer plusieurs actions sur ton téléphone."
                )
            }

            q.contains("quelle heure") || q == "heure" -> {
                val time = SimpleDateFormat(
                    "HH:mm",
                    Locale.FRANCE
                ).format(Date())
                reply("Il est $time.")
            }

            q.contains("quelle date") ||
                q.contains("quel jour sommes-nous") ||
                q.contains("on est quel jour") -> {
                val date = SimpleDateFormat(
                    "EEEE d MMMM yyyy",
                    Locale.FRENCH
                ).format(Date())
                reply("Nous sommes le $date.")
            }

            q.contains("ouvre les paramètres") ||
                q.contains("ouvre les reglages") -> {
                openSystemSettings(
                    android.provider.Settings.ACTION_SETTINGS,
                    "J'ouvre les paramètres."
                )
            }

            q.contains("ouvre le wifi") ||
                q.contains("ouvre les paramètres wifi") ||
                q.contains("ouvre le wi-fi") -> {
                openSystemSettings(
                    android.provider.Settings.ACTION_WIFI_SETTINGS,
                    "J'ouvre les paramètres Wi-Fi."
                )
            }

            q.contains("ouvre bluetooth") -> {
                openSystemSettings(
                    android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,
                    "J'ouvre les paramètres Bluetooth."
                )
            }

            q.contains("ouvre youtube") -> {
                openWeb("https://www.youtube.com")
            }

            q.contains("ouvre google") ||
                q.contains("ouvre internet") ||
                q.contains("ouvre chrome") -> {
                openWeb("https://www.google.com")
            }

            q.contains("ouvre appareil photo") ||
                q.contains("ouvre la caméra") -> {
                runCatching {
                    context.startActivity(
                        Intent("android.media.action.IMAGE_CAPTURE")
                    )
                    reply("J'ouvre l'appareil photo.")
                }.onFailure {
                    reply("Je ne peux pas ouvrir l'appareil photo.")
                }
            }

            q.startsWith("recherche ") ||
                q.startsWith("cherche sur internet ") -> {
                val search = if (q.startsWith("recherche ")) {
                    question.substringAfter("recherche ", "")
                } else {
                    question.substringAfter("cherche sur internet ", "")
                }

                openWeb(
                    "https://www.google.com/search?q=${Uri.encode(search)}"
                )
            }

            q.contains("efface la conversation") ||
                q.contains("effacer la mémoire") -> {
                history.clear()
                history.add(
                    ChatMessage(
                        "La conversation locale a été effacée.",
                        false
                    )
                )
                saveMemory()
                status = "MÉMOIRE EFFACÉE"
            }

            q.contains("merci") -> {
                reply("Avec plaisir !")
            }

            else -> {
                askOnlineAI(question)
            }
        }
    }

    // Correction importante : on conserve une référence à jour
    // vers la fonction executeCommand pour la reconnaissance vocale.
    val latestCommand by rememberUpdatedState(
        newValue = { command: String ->
            executeCommand(command)
        }
    )

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRANCE
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(0.95f)
            }
        }

        recognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    status = "J'ÉCOUTE..."
                    listening = true
                }

                override fun onBeginningOfSpeech() {
                    listening = true
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    listening = false
                    status = "TRAITEMENT..."
                }

                override fun onError(error: Int) {
                    listening = false
                    status = "SYSTÈME PRÊT"
                }

                override fun onResults(results: Bundle?) {
                    listening = false

                    val spoken = results
                        ?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        ?.firstOrNull()

                    if (!spoken.isNullOrBlank()) {
                        latestCommand(spoken)
                    } else {
                        status = "AUCUNE COMMANDE REÇUE"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )

        onDispose {
            recognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun startListening() {
        if (recognizer == null) {
            status = "RECONNAISSANCE VOCALE INDISPONIBLE"
            return
        }

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        runCatching {
            recognizer.startListening(intent)
        }.onFailure {
            status = "ERREUR MICRO"
        }
    }

    val microphonePermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListening()
            } else {
                status = "AUTORISATION MICRO REFUSÉE"
            }
        }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.lastIndex)
        }
        saveMemory()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Background,
            surface = Panel,
            onPrimary = Background,
            onBackground = MainText,
            onSurface = MainText
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(16.dp)
        ) {
            // EN-TÊTE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "J.A.R.V.I.S",
                        color = Accent,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "PERSONAL ANDROID INTELLIGENCE",
                        color = MutedText,
                        fontSize = 9.sp
                    )
                }

                TextButton(
                    onClick = { showSettings = !showSettings }
                ) {
                    Text(
                        if (showSettings) "FERMER" else "RÉGLAGES",
                        color = Accent
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ÉTAT DU SYSTÈME
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Panel,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "●",
                        color = if (listening) Color.Green else Accent
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        status,
                        color = MainText,
                        fontSize = 12.sp
                    )
                }
            }

            // PARAMÈTRES IA
            if (showSettings) {
                Spacer(Modifier.height(14.dp))

                Text(
                    "CONFIGURATION IA EN LIGNE",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("Adresse du service (compatible OpenAI)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modèle IA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Clé API") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        prefs.edit()
                            .putString(ENDPOINT_KEY, endpoint.trim())
                            .putString(MODEL_KEY, model.trim())
                            .putString(API_KEY, apiKey.trim())
                            .apply()
                        showSettings = false
                        status = "CONFIGURATION ENREGISTRÉE"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ENREGISTRER")
                }
            }

            Spacer(Modifier.height(14.dp))

            // CONVERSATION
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.fromUser) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {
                        Surface(
                            color = if (message.fromUser) Accent else Panel,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Text(
                                message.text,
                                color = if (message.fromUser) Background else MainText,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // SAISIE ET MICRO
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Parle ou écris à JARVIS…", color = MutedText) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { executeCommand(input) },
                    enabled = input.isNotBlank() && !thinking
                ) {
                    Text("ENVOYER")
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        startListening()
                    } else {
                        microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !thinking && !listening,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    when {
                        thinking -> "RÉFLEXION…"
                        listening -> "J'ÉCOUTE…"
                        else -> "🎤 PARLER À JARVIS"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
