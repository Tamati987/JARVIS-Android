package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color(0xFF030914)
private val Panel = Color(0xFF0B1B2D)
private val Accent = Color(0xFF35E7FF)
private val Blue = Color(0xFF1677FF)
private val MainText = Color(0xFFEAF7FF)
private val Muted = Color(0xFF8CA8C0)
private val Ready = Color(0xFF5CFF6B)

private const val PREFS = "jarvis_memory"
private const val HISTORY = "conversation"
private const val API_KEY = "api_key"
private const val API_MODEL = "model"
private const val API_ENDPOINT = "endpoint"

// Dimensions de l'image d'accueil (jarvis_home.jpg), pour placer les zones cliquables.
private const val HOME_W = 941f
private const val HOME_H = 1614f

data class ChatMessage(
    val text: String,
    val fromUser: Boolean
)

private enum class Screen { HOME, CHAT, TOOLS, APPS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisApp()
        }
    }
}

@Composable
fun JarvisApp() {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)
        showSplash = false
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
        if (showSplash) {
            JarvisSplash()
        } else {
            JarvisAssistant()
        }
    }
}

@Composable
fun JarvisSplash() {
    val animation = rememberInfiniteTransition(label = "splash")

    val pulse by animation.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(1300, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by animation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF09233B),
                        Background,
                        Color(0xFF01040A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(
                size.width / 2f,
                size.height * 0.42f
            )
            val radius = size.minDimension * 0.43f

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0x4422DFFF),
                        Color.Transparent
                    ),
                    center,
                    radius
                ),
                radius = radius,
                center = center
            )

            for (i in 1..5) {
                drawCircle(
                    color = Accent.copy(alpha = 0.045f),
                    radius = radius * i / 5f,
                    center = center,
                    style = Stroke(1.dp.toPx())
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(Modifier.size(270.dp)) {
                val center = Offset(
                    size.width / 2f,
                    size.height / 2f
                )
                val radius = size.minDimension * 0.37f

                drawCircle(
                    color = Accent.copy(alpha = 0.12f),
                    radius = radius * 1.5f * pulse,
                    center = center
                )

                drawCircle(
                    color = Accent,
                    radius = radius,
                    center = center,
                    style = Stroke(2.dp.toPx())
                )

                drawCircle(
                    color = Blue,
                    radius = radius * 0.78f,
                    center = center,
                    style = Stroke(5.dp.toPx())
                )

                drawArc(
                    color = Accent,
                    startAngle = rotation,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius * 1.18f,
                        center.y - radius * 1.18f
                    ),
                    size = Size(
                        radius * 2.36f,
                        radius * 2.36f
                    ),
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawArc(
                    color = Color.White,
                    startAngle = -rotation,
                    sweepAngle = 50f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius * 0.62f,
                        center.y - radius * 0.62f
                    ),
                    size = Size(
                        radius * 1.24f,
                        radius * 1.24f
                    ),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color.White,
                            Accent,
                            Blue
                        )
                    ),
                    radius = radius * 0.28f * pulse,
                    center = center
                )
            }

            Spacer(Modifier.height(35.dp))

            Text(
                "J.A.R.V.I.S",
                color = Accent,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 7.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "PERSONAL AI ASSISTANT",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(45.dp))

            Text(
                "INITIALISATION DU SYSTÈME",
                color = Accent,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(15.dp))

            Text(
                "JARVIS EST EN COURS DE DÉMARRAGE...",
                color = Muted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun JarvisAssistant() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS, 0)
    }
    val handler = remember {
        Handler(Looper.getMainLooper())
    }

    var status by remember {
        mutableStateOf("SYSTÈME EN LIGNE")
    }
    var listening by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var screen by remember { mutableStateOf(Screen.HOME) }

    var endpoint by remember {
        mutableStateOf(
            prefs.getString(
                API_ENDPOINT,
                "https://api.openai.com/v1/chat/completions"
            ) ?: ""
        )
    }

    var model by remember {
        mutableStateOf(
            prefs.getString(API_MODEL, "gpt-4o-mini") ?: ""
        )
    }

    var apiKey by remember {
        mutableStateOf(
            prefs.getString(API_KEY, "") ?: ""
        )
    }

    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            val saved = prefs.getString(HISTORY, null)

            if (!saved.isNullOrBlank()) {
                runCatching {
                    val array = JSONArray(saved)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        add(
                            ChatMessage(
                                item.optString("text"),
                                item.optBoolean("user")
                            )
                        )
                    }
                }
            }

            if (isEmpty()) {
                add(
                    ChatMessage(
                        "Bonjour ! Je suis JARVIS. Ton assistant personnel est opérationnel. Que puis-je faire pour toi ?",
                        false
                    )
                )
            }
        }
    }

    var tts by remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    fun saveHistory() {
        runCatching {
            val array = JSONArray()
            messages.takeLast(100).forEach {
                array.put(
                    JSONObject()
                        .put("text", it.text)
                        .put("user", it.fromUser)
                )
            }
            prefs.edit()
                .putString(HISTORY, array.toString())
                .apply()
        }
    }

    fun speak(text: String) {
        tts?.speak(
            text.take(3000),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_reply"
        )
    }

    fun reply(text: String) {
        messages.add(ChatMessage(text, false))
        saveHistory()
        status = "JARVIS PRÊT"
        thinking = false
        speak(text)
    }

    fun openWeb(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            )
        }.onFailure {
            reply("Impossible d'ouvrir cette page.")
        }
    }

    fun openIntent(intent: Intent, success: String, failure: String) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            reply(success)
        }.onFailure {
            reply(failure)
        }
    }

    fun openAgenda() {
        val agenda = Intent(Intent.ACTION_VIEW).setData(
            CalendarContract.CONTENT_URI.buildUpon()
                .appendPath("time")
                .appendPath(System.currentTimeMillis().toString())
                .build()
        )
        openIntent(agenda, "J'ouvre ton agenda.", "Aucune appli d'agenda trouvée.")
    }

    fun askAI(question: String) {
        if (apiKey.isBlank()) {
            reply(
                "Configure ta clé API OpenAI dans les réglages pour activer mon intelligence artificielle en ligne."
            )
            return
        }

        if (endpoint.isBlank() || model.isBlank()) {
            reply("Vérifie l'adresse du service et le modèle IA.")
            return
        }

        thinking = true
        status = "INTELLIGENCE ARTIFICIELLE..."

        val conversation = messages.takeLast(12).toList()
        val savedKey = apiKey
        val savedEndpoint = endpoint
        val savedModel = model

        Thread {
            val answer = try {
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

                val apiMessages = JSONArray()

                apiMessages.put(
                    JSONObject()
                        .put("role", "system")
                        .put(
                            "content",
                            "Tu es JARVIS, assistant personnel Android. Réponds toujours en français. Sois intelligent, poli, direct et utile. Ne prétends jamais avoir effectué une action que tu n'as pas réellement exécutée."
                        )
                )

                // La conversation contient déjà la question, ajoutée par executeCommand.
                conversation.forEach {
                    apiMessages.put(
                        JSONObject()
                            .put(
                                "role",
                                if (it.fromUser) "user" else "assistant"
                            )
                            .put("content", it.text)
                    )
                }

                val body = JSONObject()
                    .put("model", savedModel)
                    .put("messages", apiMessages)
                    .put("temperature", 0.7)

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                connection.disconnect()

                if (code in 200..299) {
                    JSONObject(response)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    "Erreur du service IA : HTTP $code."
                }
            } catch (e: Exception) {
                "Connexion impossible. Vérifie Internet et tes réglages API."
            }

            handler.post {
                reply(answer)
            }
        }.start()
    }

    fun executeCommand(raw: String) {
        val question = raw.trim()
        if (question.isBlank() || thinking) return

        messages.add(ChatMessage(question, true))
        input = ""
        saveHistory()

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
                    "Je suis JARVIS, ton assistant personnel Android. Je peux discuter avec toi, écouter tes commandes vocales et effectuer certaines actions sur ton téléphone."
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
                q.contains("quel jour sommes-nous") -> {
                val date = SimpleDateFormat(
                    "EEEE d MMMM yyyy",
                    Locale.FRENCH
                ).format(Date())
                reply("Nous sommes le $date.")
            }

            q.contains("ouvre les paramètres") ||
                q.contains("ouvre les reglages") -> {
                openIntent(
                    Intent(android.provider.Settings.ACTION_SETTINGS),
                    "J'ouvre les paramètres.",
                    "Impossible d'ouvrir les paramètres."
                )
            }

            q.contains("ouvre le wifi") ||
                q.contains("ouvre les paramètres wifi") -> {
                openIntent(
                    Intent(android.provider.Settings.ACTION_WIFI_SETTINGS),
                    "J'ouvre les paramètres Wi-Fi.",
                    "Impossible d'ouvrir le Wi-Fi."
                )
            }

            q.contains("ouvre bluetooth") -> {
                openIntent(
                    Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),
                    "J'ouvre le Bluetooth.",
                    "Impossible d'ouvrir le Bluetooth."
                )
            }

            q.contains("agenda") || q.contains("calendrier") -> {
                openAgenda()
            }

            q.contains("ouvre youtube") -> {
                openWeb("https://www.youtube.com")
            }

            q.contains("ouvre google") ||
                q.contains("ouvre chrome") ||
                q.contains("ouvre internet") -> {
                openWeb("https://www.google.com")
            }

            q.contains("ouvre appareil photo") ||
                q.contains("ouvre la caméra") -> {
                openIntent(
                    Intent("android.media.action.IMAGE_CAPTURE"),
                    "J'ouvre l'appareil photo.",
                    "Impossible d'ouvrir l'appareil photo."
                )
            }

            q.startsWith("recherche ") -> {
                val search = question.substringAfter("recherche ")
                openWeb(
                    "https://www.google.com/search?q=${Uri.encode(search)}"
                )
            }

            q.contains("efface la conversation") -> {
                messages.clear()
                messages.add(
                    ChatMessage(
                        "Conversation effacée.",
                        false
                    )
                )
                saveHistory()
                status = "MÉMOIRE EFFACÉE"
            }

            q.contains("merci") -> {
                reply("Avec plaisir !")
            }

            else -> askAI(question)
        }
    }

    val latestCommand by rememberUpdatedState(
        newValue = { command: String ->
            executeCommand(command)
        }
    )

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

        // Jarvis se tait pendant qu'on lui parle.
        tts?.stop()

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

    fun onMicrophone() {
        if (thinking) return
        if (listening) {
            recognizer?.stopListening()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startListening()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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

    // Retour : revient à l'accueil au lieu de quitter l'appli.
    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
    }

    val lastReply = messages.lastOrNull { !it.fromUser }?.text.orEmpty()

    when (screen) {
        Screen.HOME -> HomeScreen(
            status = status,
            lastReply = lastReply,
            listening = listening,
            thinking = thinking,
            onMicrophone = { onMicrophone() },
            onControlPhone = { screen = Screen.TOOLS },
            onOpenApp = { screen = Screen.APPS },
            onQuestions = { screen = Screen.CHAT },
            onAgenda = { openAgenda() },
            onFunctions = { screen = Screen.CHAT },
            onTools = { screen = Screen.TOOLS },
            onSettings = { screen = Screen.SETTINGS }
        )

        Screen.CHAT -> ChatScreen(
            messages = messages,
            input = input,
            onInput = { input = it },
            thinking = thinking,
            listening = listening,
            status = status,
            onSend = { executeCommand(input) },
            onMicrophone = { onMicrophone() },
            onBack = { screen = Screen.HOME }
        )

        Screen.TOOLS -> ToolsScreen(
            onCommand = {
                screen = Screen.CHAT
                executeCommand(it)
            },
            onBack = { screen = Screen.HOME }
        )

        Screen.APPS -> AppsScreen(
            onOpened = { name ->
                messages.add(ChatMessage("J'ouvre $name.", false))
                saveHistory()
                speak("J'ouvre $name.")
            },
            onBack = { screen = Screen.HOME }
        )

        Screen.SETTINGS -> SettingsScreen(
            endpoint = endpoint,
            model = model,
            apiKey = apiKey,
            onEndpoint = { endpoint = it },
            onModel = { model = it },
            onApiKey = { apiKey = it },
            onSave = {
                prefs.edit()
                    .putString(API_ENDPOINT, endpoint.trim())
                    .putString(API_MODEL, model.trim())
                    .putString(API_KEY, apiKey.trim())
                    .apply()
                status = "CONFIGURATION ENREGISTRÉE"
                screen = Screen.HOME
            },
            onClear = {
                messages.clear()
                messages.add(ChatMessage("Conversation effacée.", false))
                saveHistory()
                status = "MÉMOIRE EFFACÉE"
            },
            onBack = { screen = Screen.HOME }
        )
    }
}

/**
 * Écran d'accueil : l'image J.A.R.V.I.S en fond, avec des zones cliquables placées sur
 * ses boutons (tuiles, micro, barre du bas, roue des réglages) et l'état réel de Jarvis
 * affiché dans le cadre « Jarvis est prêt ».
 */
@Composable
private fun HomeScreen(
    status: String,
    lastReply: String,
    listening: Boolean,
    thinking: Boolean,
    onMicrophone: () -> Unit,
    onControlPhone: () -> Unit,
    onOpenApp: () -> Unit,
    onQuestions: () -> Unit,
    onAgenda: () -> Unit,
    onFunctions: () -> Unit,
    onTools: () -> Unit,
    onSettings: () -> Unit
) {
    val animation = rememberInfiniteTransition(label = "micro")
    val glow by animation.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HOME_W / HOME_H)
        ) {
            val sx = maxWidth / HOME_W
            val sy = maxHeight / HOME_H

            Image(
                painter = painterResource(R.drawable.jarvis_home),
                contentDescription = "Accueil J.A.R.V.I.S",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            // Zone cliquable placée sur un élément de l'image (coordonnées en pixels de l'image).
            @Composable
            fun Spot(x1: Float, y1: Float, x2: Float, y2: Float, round: Boolean = false, onClick: () -> Unit) {
                Box(
                    Modifier
                        .offset(x = sx * x1, y = sy * y1)
                        .size(width = sx * (x2 - x1), height = sy * (y2 - y1))
                        .clip(if (round) CircleShape else RoundedCornerShape(14.dp))
                        .clickable(onClick = onClick)
                )
            }

            Spot(840f, 45f, 925f, 120f, round = true, onClick = onSettings)
            Spot(25f, 232f, 218f, 404f, onClick = onControlPhone)
            Spot(722f, 232f, 917f, 404f, onClick = onOpenApp)
            Spot(25f, 434f, 218f, 602f, onClick = onQuestions)
            Spot(722f, 434f, 917f, 602f, onClick = onAgenda)

            // Micro : halo animé pendant l'écoute ou la réflexion.
            if (listening || thinking) {
                Box(
                    Modifier
                        .offset(x = sx * 330f, y = sy * 967f)
                        .size(width = sx * 280f, height = sx * 280f)
                        .clip(CircleShape)
                        .background((if (listening) Ready else Accent).copy(alpha = glow * 0.35f))
                )
            }
            Spot(345f, 982f, 595f, 1232f, round = true, onClick = onMicrophone)

            // Cadre d'état : on recouvre le texte de l'image avec l'état réel de Jarvis.
            Box(
                Modifier
                    .offset(x = sx * 340f, y = sy * 1272f)
                    .size(width = sx * 530f, height = sy * 112f)
                    .background(Color(0xFF00050C)),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(Modifier.padding(start = 4.dp)) {
                    Text(
                        when {
                            listening -> "Je vous écoute…"
                            thinking -> "Je réfléchis…"
                            else -> "Jarvis est prêt !"
                        },
                        color = if (thinking) Accent else Ready,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        if (listening || thinking || lastReply.isBlank()) status.lowercase(Locale.FRENCH)
                            .replaceFirstChar { it.uppercase() } else lastReply,
                        color = MainText,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Barre du bas : Accueil, Fonctions, masque (micro), Outils, Paramètres.
            Spot(40f, 1442f, 175f, 1582f) {}
            Spot(215f, 1442f, 350f, 1582f, onClick = onFunctions)
            Spot(395f, 1430f, 545f, 1590f, round = true, onClick = onMicrophone)
            Spot(595f, 1442f, 730f, 1582f, onClick = onTools)
            Spot(760f, 1442f, 915f, 1582f, onClick = onSettings)
        }
    }
}

@Composable
private fun JarvisTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("← ACCUEIL", color = Accent)
        }
        Spacer(Modifier.weight(1f))
        Text(
            title,
            color = Accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun JarvisPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF09233B), Background, Color(0xFF01040A)))
            )
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        JarvisTopBar(title, onBack)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ChatScreen(
    messages: List<ChatMessage>,
    input: String,
    onInput: (String) -> Unit,
    thinking: Boolean,
    listening: Boolean,
    status: String,
    onSend: () -> Unit,
    onMicrophone: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    JarvisPage("CONVERSATION", onBack) {
        Text(status, color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                placeholder = { Text("Écris à JARVIS…", color = Muted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onSend,
                enabled = input.isNotBlank() && !thinking
            ) {
                Text("ENVOYER")
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onMicrophone,
            enabled = !thinking,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                when {
                    thinking -> "RÉFLEXION…"
                    listening -> "J'ÉCOUTE… (TOUCHER POUR ARRÊTER)"
                    else -> "🎤 PARLER À JARVIS"
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ToolsScreen(onCommand: (String) -> Unit, onBack: () -> Unit) {
    val tools = listOf(
        "Paramètres" to "ouvre les paramètres",
        "Wi-Fi" to "ouvre le wifi",
        "Bluetooth" to "ouvre bluetooth",
        "Appareil photo" to "ouvre la caméra",
        "Agenda" to "ouvre mon agenda",
        "YouTube" to "ouvre youtube",
        "Google" to "ouvre google",
        "Quelle heure est-il ?" to "quelle heure",
        "Quelle date ?" to "quelle date",
        "Effacer la conversation" to "efface la conversation"
    )

    JarvisPage("OUTILS", onBack) {
        Text("Contrôler mon téléphone", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tools) { (label, command) ->
                Surface(
                    color = Panel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCommand(command) }
                ) {
                    Text(
                        label,
                        color = MainText,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsScreen(onOpened: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val apps = remember {
        val pm = context.packageManager
        pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0
        )
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .filter { it.second != context.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase(Locale.FRENCH) }
    }

    JarvisPage("APPLICATIONS", onBack) {
        Text("Ouvrir une application", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(apps) { (label, pkg) ->
                Surface(
                    color = Panel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                                context.startActivity(it)
                                onOpened(label)
                            }
                        }
                ) {
                    Text(
                        label,
                        color = MainText,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    endpoint: String,
    model: String,
    apiKey: String,
    onEndpoint: (String) -> Unit,
    onModel: (String) -> Unit,
    onApiKey: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    JarvisPage("PARAMÈTRES", onBack) {
        Text(
            "CONFIGURATION IA EN LIGNE",
            color = Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = endpoint,
            onValueChange = onEndpoint,
            label = { Text("Adresse du service (compatible OpenAI)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = onModel,
            label = { Text("Modèle IA") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKey,
            label = { Text("Clé API") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("ENREGISTRER")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
            Text("EFFACER LA CONVERSATION", color = Accent)
        }
        Spacer(Modifier.height(8.dp))
        val context = LocalContext.current
        var wallpaperDone by remember { mutableStateOf(false) }
        // L'image d'accueil de Jarvis devient le fond d'écran du téléphone (accueil et verrouillage).
        OutlinedButton(
            onClick = {
                wallpaperDone = runCatching {
                    android.app.WallpaperManager.getInstance(context).setResource(R.drawable.jarvis_home)
                }.isSuccess
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (wallpaperDone) "FOND D'ÉCRAN APPLIQUÉ ✓" else "METTRE JARVIS EN FOND D'ÉCRAN",
                color = Accent
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Jarvis fonctionne sans clé pour ses commandes intégrées (heure, date, Wi-Fi, Bluetooth, agenda, applis…). " +
                "La clé API active ses réponses intelligentes à toutes les questions.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Start
        )
    }
}
