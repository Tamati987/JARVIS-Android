
package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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

private val Background = Color(0xFF07111F)
private val Panel = Color(0xFF102238)
private val Accent = Color(0xFF22D3EE)
private val MainText = Color(0xFFEAF7FF)
private val MutedText = Color(0xFF9BB2C8)

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
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("jarvis_settings", 0)
    }

    var input by remember { mutableStateOf("") }
    var aiEndpoint by remember {
        mutableStateOf(prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "")
    }
    var aiModel by remember {
        mutableStateOf(prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini")
    }
    var aiKey by remember {
        mutableStateOf(prefs.getString("key", "") ?: "")
    }
    var showSettings by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Système prêt") }

    val history = remember {
        mutableStateListOf<ChatMessage>().apply {
            add(ChatMessage("Bonjour. Je suis JARVIS. Comment puis-je vous aider ?", false))
        }
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    fun addReply(message: String) {
        history.add(ChatMessage(message, false))
        speak(message)
        status = "Réponse prête"
    }

    fun openApp(packageName: String, label: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            addReply("J'ouvre $label.")
        } else {
            addReply("Je ne trouve pas l'application $label sur cet appareil.")
        }
    }

    fun askOnlineAI(question: String) {
        if (aiKey.isBlank()) {
            addReply("Pour utiliser l'intelligence artificielle en ligne, configure d'abord ta clé API dans les paramètres.")
            return
        }

        history.add(ChatMessage("Connexion à l'intelligence artificielle...", false))
        status = "Connexion IA..."

        Thread {
            var answer = "Je n'ai pas réussi à contacter le service IA."
            try {
                val url = URL(aiEndpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $aiKey")

                val messages = JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", "Tu es JARVIS, un assistant personnel utile. Réponds en français, clairement et naturellement.")
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", question)
                    )

                val body = JSONObject()
                    .put("model", aiModel)
                    .put("messages", messages)
                    .put("temperature", 0.7)

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream.bufferedReader().use { it.readText() }
                if (responseCode in 200..299) {
                    val json = JSONObject(response)
                    answer = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    answer = "Erreur du service IA ($responseCode). Vérifie ton endpoint et ta clé API."
                }
                connection.disconnect()
            } catch (e: Exception) {
                answer = "Connexion impossible : ${e.localizedMessage ?: "erreur inconnue"}"
            }

            (context as? ComponentActivity)?.runOnUiThread {
                history.add(ChatMessage(answer, false))
                speak(answer)
                status = "Système prêt"
            }
        }.start()
    }

    fun respond(rawText: String) {
        val question = rawText.trim()
        if (question.isEmpty()) return

        history.add(ChatMessage(question, true))
        input = ""
        val q = question.lowercase(Locale.FRENCH)

        when {
            q.contains("bonjour") || q == "salut" || q.contains("bonsoir") ->
                addReply("Bonjour ! Je suis à ton écoute.")

            q.contains("quelle heure") || q == "heure" ->
                addReply("Il est ${SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date())}.")

            q.contains("quelle date") || q.contains("on est quel jour") ->
                addReply("Nous sommes le ${SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(Date())}.")

            q.contains("ouvre les paramètres") || q.contains("ouvre les reglages") ->
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    addReply("J'ouvre les paramètres.")
                }.onFailure { addReply("Je ne peux pas ouvrir les paramètres.") }

            q.contains("wifi") || q.contains("wi-fi") ->
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    addReply("J'ouvre les paramètres Wi-Fi.")
                }.onFailure { addReply("Je ne peux pas ouvrir les paramètres Wi-Fi.") }

            q.contains("bluetooth") ->
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    addReply("J'ouvre les paramètres Bluetooth.")
                }.onFailure { addReply("Je ne peux pas ouvrir les paramètres Bluetooth.") }

            q.contains("ouvre youtube") ->
                openApp("com.google.android.youtube", "YouTube")

            q.contains("ouvre chrome") || q.contains("ouvre internet") ->
                openApp("com.android.chrome", "Chrome")

            q.contains("ouvre appareil photo") || q.contains("ouvre la caméra") ->
                runCatching {
                    context.startActivity(Intent("android.media.action.IMAGE_CAPTURE"))
                    addReply("J'ouvre l'appareil photo.")
                }.onFailure { addReply("Impossible d'ouvrir l'appareil photo.") }

            q.startsWith("recherche ") || q.startsWith("cherche sur internet ") -> {
                val search = question
                    .substringAfter("recherche ", "")
                    .ifBlank { question.substringAfter("cherche sur internet ", "") }

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(search)}")
                )
                runCatching {
                    context.startActivity(intent)
                    addReply("Voici les résultats de ta recherche.")
                }.onFailure {
                    addReply("Je ne peux pas ouvrir le navigateur.")
                }
            }

            q.contains("qui es-tu") || q.contains("qui est jarvis") ->
                addReply("Je suis JARVIS, ton assistant personnel Android.")

            q.contains("merci") ->
                addReply("Avec plaisir !")

            else -> askOnlineAI(question)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            status = "Micro autorisé"
            listening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            speechRecognizer?.startListening(intent)
        } else {
            status = "Permission micro refusée"
        }
    }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRANCE
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    status = "Je t'écoute..."
                }

                override fun onBeginningOfSpeech() {
                    listening = true
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    listening = false
                    status = "Traitement..."
                }

                override fun onError(error: Int) {
                    listening = false
                    status = "Micro prêt"
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val spokenText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()

                    if (!spokenText.isNullOrBlank()) {
                        respond(spokenText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        onDispose {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("J.A.R.V.I.S", color = Accent, fontSize = 27.sp)
                    Text("ASSISTANT PERSONNEL", color = MutedText, fontSize = 10.sp)
                }

                TextButton(onClick = { showSettings = !showSettings }) {
                    Text(if (showSettings) "FERMER" else "PARAMÈTRES")
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Accent, RoundedCornerShape(50))
                )
                Spacer(Modifier.width(10.dp))
                Text(status, color = MutedText, fontSize = 13.sp)
            }

            if (showSettings) {
                Spacer(Modifier.height(12.dp))
                Text("CONFIGURATION IA", color = Accent, fontSize = 14.sp)

                BasicTextField(
                    value = aiEndpoint,
                    onValueChange = { aiEndpoint = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Panel, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    textStyle = TextStyle(color = MainText, fontSize = 13.sp),
                    decorationBox = { inner ->
                        if (aiEndpoint.isEmpty()) {
                            Text("Adresse API", color = MutedText)
                        }
                        inner()
                    }
                )

                Spacer(Modifier.height(8.dp))

                BasicTextField(
                    value = aiModel,
                    onValueChange = { aiModel = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Panel, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    textStyle = TextStyle(color = MainText, fontSize = 13.sp),
                    decorationBox = { inner ->
                        if (aiModel.isEmpty()) {
                            Text("Modèle IA", color = MutedText)
                        }
                        inner()
                    }
                )

                Spacer(Modifier.height(8.dp))

                BasicTextField(
                    value = aiKey,
                    onValueChange = { aiKey = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Panel, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    textStyle = TextStyle(color = MainText, fontSize = 13.sp),
                    decorationBox = { inner ->
                        if (aiKey.isEmpty()) {
                            Text("Clé API", color = MutedText)
                        }
                        inner()
                    }
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        prefs.edit()
                            .putString("endpoint", aiEndpoint)
                            .putString("model", aiModel)
                            .putString("key", aiKey)
                            .apply()
                        status = "Configuration enregistrée"
                        showSettings = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ENREGISTRER")
                }

                Text(
                    "Attention : la clé API est stockée localement sur cet appareil. Ne partage pas ton fichier APK avec une clé personnelle enregistrée.",
                    color = MutedText,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text("CONVERSATION", color = Accent, fontSize = 13.sp)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Text(
                            message.text,
                            color = MainText,
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .background(
                                    if (message.fromUser) Color(0xFF16465B) else Panel,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(Panel, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    textStyle = TextStyle(color = MainText, fontSize = 14.sp),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text("Écris à JARVIS...", color = MutedText)
                        }
                        inner()
                    }
                )

                Spacer(Modifier.width(6.dp))

                Button(
                    onClick = { respond(input) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text("ENVOYER")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
 
