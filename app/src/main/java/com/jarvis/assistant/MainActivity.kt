
package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val messages = remember {
        mutableStateListOf(
            "JARVIS : Bonjour. Je suis prêt à vous écouter."
        )
    }
    var userText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("SYSTÈME EN LIGNE") }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun respond(text: String) {
        val question = text.lowercase(Locale.FRENCH)
        val answer = when {
            "bonjour" in question ->
                "Bonjour. Heureux de vous retrouver."

            "qui es-tu" in question || "qui es tu" in question ->
                "Je suis JARVIS, votre assistant personnel."

            "ton nom" in question ->
                "Je m'appelle JARVIS."

            "heure" in question -> {
                val time = SimpleDateFormat(
                    "HH:mm",
                    Locale.FRANCE
                ).format(Date())
                "Il est actuellement $time."
            }

            "date" in question -> {
                val date = SimpleDateFormat(
                    "EEEE d MMMM yyyy",
                    Locale.FRANCE
                ).format(Date())
                "Nous sommes le $date."
            }

            "merci" in question ->
                "Avec plaisir. Je reste à votre disposition."

            "au revoir" in question ->
                "À bientôt. JARVIS reste disponible."

            else ->
                "J'ai bien reçu votre message : $text. " +
                    "Mon intelligence artificielle complète sera ajoutée dans une prochaine étape."
        }

        messages.add("VOUS : $text")
        messages.add("JARVIS : $answer")
        tts?.speak(
            answer,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_response"
        )
    }

    val microphonePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez à JARVIS")
            }
            recognizer?.startListening(intent)
        } else {
            status = "AUTORISATION MICRO REFUSÉE"
        }
    }

    DisposableEffect(context, recognizer) {
        val engine = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRANCE
            }
        }
        tts = engine
        engine.language = Locale.FRANCE

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
                status = "ANALYSE DU MESSAGE..."
            }

            override fun onError(error: Int) {
                status = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "AUCUNE PAROLE DÉTECTÉE"

                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "AUTORISATION MICRO REQUISE"

                    else -> "ÉCOUTE TERMINÉE — RÉESSAYEZ"
                }
            }

            override fun onResults(results: Bundle?) {
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {
                    userText = spokenText
                    respond(spokenText)
                    status = "SYSTÈME EN LIGNE"
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "J.A.R.V.I.S",
                color = Color(0xFF55DFFF),
                fontSize = 32.sp
            )

            Text(
                text = "ASSISTANT PERSONNEL",
                color = Color.LightGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "◉",
                color = Color(0xFF55DFFF),
                fontSize = 76.sp
            )

            Text(
                text = status,
                color = Color(0xFF55DFFF),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Color(0xFF101F31),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    Text(
                        text = message,
                        color = if (message.startsWith("JARVIS"))
                            Color(0xFF7DEBFF)
                        else Color.White,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = userText,
                    onValueChange = { userText = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            Color(0xFF182B40),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (userText.isEmpty()) {
                                Text(
                                    "Écris à JARVIS...",
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val message = userText.trim()
                        if (message.isNotEmpty()) {
                            respond(message)
                            userText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF087EAB)
                    )
                ) {
                    Text("ENVOYER")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (recognizer == null) {
                        status = "RECONNAISSANCE VOCALE INDISPONIBLE"
                    } else if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val intent = Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                        ).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE,
                                "fr-FR"
                            )
                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "Parlez à JARVIS"
                            )
                        }
                        recognizer.startListening(intent)
                    } else {
                        microphonePermission.launch(
                            Manifest.permission.RECORD_AUDIO
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF075E79)
                )
            ) {
                Text("🎙  PARLER À JARVIS")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
