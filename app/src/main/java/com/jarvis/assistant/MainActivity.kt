
package com.jarvis.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF06111F)
private val Cyan = Color(0xFF36E5FF)
private val Muted = Color(0xFF9DB5C9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                JarvisScreen()
            }
        }
    }
}

@Composable
fun JarvisScreen() {
    var question by remember { mutableStateOf("") }
    var response by remember {
        mutableStateOf("Bonjour. Je suis JARVIS, votre assistant personnel.")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF10283D),
                            Background,
                            Color(0xFF02070D)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "J.A.R.V.I.S",
                color = Cyan,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp
            )

            Text(
                text = "ASSISTANT PERSONNEL INTELLIGENT",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(190.dp)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                Cyan,
                                Color(0xFF1265A8),
                                Color(0xFF142B48),
                                Cyan
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(12.dp)
                    .border(
                        width = 1.dp,
                        color = Cyan.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF1A6688),
                                Color(0xFF0A243A),
                                Color(0xFF06111F)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = Cyan,
                    fontSize = 78.sp,
                    fontWeight = FontWeight.ExtraLight
                )
            }

            Text(
                text = "SYSTÈME EN LIGNE",
                color = Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Surface(
                color = Color(0xFF0D2030),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = response,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    modifier = Modifier.padding(18.dp)
                )
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Écrivez à JARVIS") },
                placeholder = { Text("Que souhaitez-vous faire ?") },
                shape = RoundedCornerShape(14.dp),
                singleLine = false
            )

            Button(
                onClick = {
                    response = if (question.isBlank()) {
                        "Je vous écoute. Écrivez d'abord votre demande."
                    } else {
                        "Votre demande : \"$question\"\n\nMon interface est prête. La connexion à une véritable IA sera ajoutée dans une prochaine étape."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cyan,
                    contentColor = Color(0xFF03111D)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "ENVOYER À JARVIS",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "JARVIS • PROTOTYPE 1.0",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
