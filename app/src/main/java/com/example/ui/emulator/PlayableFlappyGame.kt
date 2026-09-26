package com.example.ui.emulator

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.Android9VirtualEnvironment
import com.example.model.Android9VirtualApp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.TechCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

data class FlappyPipe(
    var x: Float,
    val topHeight: Float,
    val gap: Float = 220f,
    var scored: Boolean = false
)

@Composable
fun PlayableFlappyGame(
    app: Android9VirtualApp,
    modifier: Modifier = Modifier
) {
    var gameState by remember { mutableStateOf("READY") } // READY, RUNNING, GAMEOVER
    var birdY by remember { mutableFloatStateOf(300f) }
    var birdVelocity by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var framesRendered by remember { mutableLongStateOf(0L) }
    var houdiniTranslationsCount by remember { mutableLongStateOf(0L) }
    var groundOffset by remember { mutableFloatStateOf(0f) }

    val pipes = remember { mutableStateListOf<FlappyPipe>() }

    val gravity = 0.85f
    val jumpStrength = -13.5f
    val pipeSpeed = 3.6f

    fun restartGame() {
        birdY = 300f
        birdVelocity = 0f
        score = 0
        pipes.clear()
        // Initialize two pipes
        pipes.add(FlappyPipe(x = 600f, topHeight = 180f))
        pipes.add(FlappyPipe(x = 950f, topHeight = 260f))
        gameState = "RUNNING"
        Android9VirtualEnvironment.addLog("I", "FlappyBird", "Game loop started. ARMv7 native JNI engine online.")
    }

    fun jump() {
        if (gameState == "READY") {
            restartGame()
        } else if (gameState == "RUNNING") {
            birdVelocity = jumpStrength
            houdiniTranslationsCount += 4
            Android9VirtualEnvironment.addLog("D", "Houdini64", "swi/svc 0x42: bird_jump_physics() [ARMv7 -> ARM64 JIT]")
        } else if (gameState == "GAMEOVER") {
            restartGame()
        }
    }

    // 60 FPS Game Loop
    LaunchedEffect(gameState) {
        if (gameState == "RUNNING") {
            while (isActive && gameState == "RUNNING") {
                delay(16L) // ~60 FPS
                framesRendered++
                groundOffset = (groundOffset + pipeSpeed) % 40f

                // Physics update
                birdVelocity += gravity
                birdY += birdVelocity

                // Ground & ceiling collision
                if (birdY < 20f) {
                    birdY = 20f
                    birdVelocity = 0f
                }
                if (birdY > 620f) {
                    birdY = 620f
                    gameState = "GAMEOVER"
                    if (score > highScore) highScore = score
                    Android9VirtualEnvironment.addLog("W", "FlappyBird", "Collision detected: Ground hit! Final Score: $score")
                }

                // Update pipes
                val pipeWidth = 70f
                val birdSize = 28f
                val birdX = 120f

                for (i in pipes.indices) {
                    val pipe = pipes[i]
                    pipe.x -= pipeSpeed

                    // Score check
                    if (!pipe.scored && pipe.x + pipeWidth < birdX) {
                        pipe.scored = true
                        score++
                        houdiniTranslationsCount += 12
                        Android9VirtualEnvironment.addLog("I", "FlappyBird", "Score point recorded: $score. JNI state synchronized.")
                    }

                    // Collision check
                    val inHorizontalRange = (birdX + birdSize > pipe.x) && (birdX < pipe.x + pipeWidth)
                    if (inHorizontalRange) {
                        val hitTop = birdY < pipe.topHeight
                        val hitBottom = birdY + birdSize > (pipe.topHeight + pipe.gap)
                        if (hitTop || hitBottom) {
                            gameState = "GAMEOVER"
                            if (score > highScore) highScore = score
                            Android9VirtualEnvironment.addLog("W", "Houdini64", "Pipe collision @ x=${pipe.x.toInt()}. Game Over.")
                        }
                    }
                }

                // Recycle pipes
                if (pipes.isNotEmpty() && pipes.first().x < -100f) {
                    pipes.removeAt(0)
                    val lastX = pipes.lastOrNull()?.x ?: 500f
                    val newHeight = 120f + Random.nextFloat() * 260f
                    pipes.add(FlappyPipe(x = lastX + 320f, topHeight = newHeight))
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // --- Game HUD Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (gameState == "RUNNING") AccentGreen else AccentAmber)
                )
                Text(
                    text = "FLAPPY BIRD 32-BIT (ARMv7)",
                    color = TechCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Houdini JIT: 60 FPS • Çeviri: $houdiniTranslationsCount",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- Live Playable Canvas Viewport ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF334155))
                .pointerInput(Unit) {
                    detectTapGestures {
                        jump()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Sky Background Gradient
                drawRect(
                    color = Color(0xFF4EC0CA),
                    size = Size(canvasWidth, canvasHeight - 70f)
                )

                // Distant Clouds
                drawCircle(color = Color(0x66FFFFFF), radius = 45f, center = Offset(canvasWidth * 0.25f, 90f))
                drawCircle(color = Color(0x66FFFFFF), radius = 60f, center = Offset(canvasWidth * 0.35f, 80f))
                drawCircle(color = Color(0x66FFFFFF), radius = 40f, center = Offset(canvasWidth * 0.75f, 130f))

                // Render Pipes
                for (pipe in pipes) {
                    val pipeWidth = 65f
                    // Top pipe
                    drawRect(
                        color = Color(0xFF73BF2E),
                        topLeft = Offset(pipe.x, 0f),
                        size = Size(pipeWidth, pipe.topHeight)
                    )
                    // Top pipe rim
                    drawRect(
                        color = Color(0xFF5BA320),
                        topLeft = Offset(pipe.x - 4f, pipe.topHeight - 24f),
                        size = Size(pipeWidth + 8f, 24f)
                    )

                    // Bottom pipe
                    val bottomY = pipe.topHeight + pipe.gap
                    drawRect(
                        color = Color(0xFF73BF2E),
                        topLeft = Offset(pipe.x, bottomY),
                        size = Size(pipeWidth, canvasHeight - bottomY - 70f)
                    )
                    // Bottom pipe rim
                    drawRect(
                        color = Color(0xFF5BA320),
                        topLeft = Offset(pipe.x - 4f, bottomY),
                        size = Size(pipeWidth + 8f, 24f)
                    )
                }

                // Render Ground
                drawRect(
                    color = Color(0xFFDED895),
                    topLeft = Offset(0f, canvasHeight - 70f),
                    size = Size(canvasWidth, 70f)
                )
                // Grass top of ground
                drawRect(
                    color = Color(0xFF73BF2E),
                    topLeft = Offset(0f, canvasHeight - 70f),
                    size = Size(canvasWidth, 14f)
                )

                // Render Animated Bird (ARMv7 32-bit Sprite)
                val birdX = 120f
                val actualBirdY = birdY.coerceIn(20f, canvasHeight - 95f)

                // Bird body
                drawCircle(
                    color = Color(0xFFF7D02C),
                    radius = 16f,
                    center = Offset(birdX, actualBirdY)
                )
                // Wing
                drawOval(
                    color = Color(0xFFE8981F),
                    topLeft = Offset(birdX - 14f, actualBirdY - 4f),
                    size = Size(16f, 10f)
                )
                // Big Cartoon Eye
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(birdX + 7f, actualBirdY - 6f)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 3f,
                    center = Offset(birdX + 8f, actualBirdY - 6f)
                )
                // Beak
                drawRect(
                    color = Color(0xFFE86100),
                    topLeft = Offset(birdX + 12f, actualBirdY),
                    size = Size(10f, 6f)
                )
            }

            // Score Banner
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SKOR: $score  •  EN İYİ: $highScore",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Overlays: Ready or Game Over
            if (gameState == "READY") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "FLAPPY BIRD 32-BIT",
                            color = Color(0xFFF7D02C),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Houdini İkili Çeviri ile Doğrudan Yürütülüyor",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = { restartGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_flappy_start")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BAŞLA / UÇUR", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Ekrana dokunun veya alttaki Zıpla butonuna basın!",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            } else if (gameState == "GAMEOVER") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("OYUN BİTTİ!", color = Color(0xFFFF5252), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Skorunuz: $score", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("En Yüksek Skor: $highScore", color = AccentAmber, fontSize = 13.sp)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "32-Bit ARMv7 Houdini JNI Motoru:\nÇevrilen Toplam Talimat: ${framesRendered * 60 + houdiniTranslationsCount * 8}",
                                    color = TechCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Button(
                                onClick = { restartGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_flappy_restart")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("YENİDEN OYNA", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Interactive Controller Bar (Touch Buttons) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { jump() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_flappy_flap")
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ZIPLA / KANAT ÇIRP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { restartGame() },
                modifier = Modifier.size(48.dp).testTag("btn_flappy_quick_restart")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Yeniden Başlat", tint = TechCyan)
            }
        }
    }
}
