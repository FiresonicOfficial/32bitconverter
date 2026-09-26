package com.example.ui.emulator

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs
import kotlin.random.Random

data class ArcadeBrick(
    val row: Int,
    val col: Int,
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    val color: Color,
    val points: Int,
    var isDestroyed: Boolean = false
)

@Composable
fun PlayableArcadeGame(
    app: Android9VirtualApp,
    modifier: Modifier = Modifier
) {
    var gameState by remember { mutableStateOf("READY") } // READY, RUNNING, GAMEOVER, VICTORY
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var level by remember { mutableIntStateOf(1) }

    var paddleX by remember { mutableFloatStateOf(200f) }
    var paddleWidth by remember { mutableFloatStateOf(110f) }

    var ballX by remember { mutableFloatStateOf(250f) }
    var ballY by remember { mutableFloatStateOf(450f) }
    var ballVx by remember { mutableFloatStateOf(4.5f) }
    var ballVy by remember { mutableFloatStateOf(-5.5f) }

    var arm32RegisterTelemetry by remember { mutableStateOf("R0: 0x0000 | R1: 0x0000 | PC: 0x000104a0") }
    var framesRendered by remember { mutableLongStateOf(0L) }
    var translationsCount by remember { mutableLongStateOf(0L) }

    val bricks = remember { mutableStateListOf<ArcadeBrick>() }

    fun initBricks(canvasW: Float) {
        bricks.clear()
        val rows = 4
        val cols = 6
        val padding = 6f
        val topMargin = 70f
        val brickWidth = (canvasW - (cols + 1) * padding) / cols
        val brickHeight = 22f

        val colors = listOf(
            Color(0xFFFF5252),
            Color(0xFFFFAB00),
            Color(0xFF00E676),
            Color(0xFF00B0FF)
        )

        for (r in 0 until rows) {
            val color = colors[r % colors.size]
            val points = (4 - r) * 10
            for (c in 0 until cols) {
                val bx = padding + c * (brickWidth + padding)
                val by = topMargin + r * (brickHeight + padding)
                bricks.add(
                    ArcadeBrick(
                        row = r,
                        col = c,
                        x = bx,
                        y = by,
                        width = brickWidth,
                        height = brickHeight,
                        color = color,
                        points = points
                    )
                )
            }
        }
    }

    fun restartGame() {
        score = 0
        lives = 3
        level = 1
        paddleX = 200f
        paddleWidth = 110f
        ballX = 250f
        ballY = 450f
        ballVx = 4.5f * if (Random.nextBoolean()) 1f else -1f
        ballVy = -5.5f
        gameState = "RUNNING"
        Android9VirtualEnvironment.addLog("I", "Arcade32", "Breakout Engine started. JNI native physics loop active.")
    }

    // 60 FPS Game Loop
    LaunchedEffect(gameState) {
        if (gameState == "RUNNING") {
            while (isActive && gameState == "RUNNING") {
                delay(16L) // ~60 FPS
                framesRendered++

                // Ball movement
                ballX += ballVx
                ballY += ballVy

                // Update simulated ARM32 registers
                if (framesRendered % 15 == 0L) {
                    val r0 = score
                    val r1 = ballX.toInt()
                    val r2 = ballY.toInt()
                    val pc = 0x00010000 + (framesRendered % 0xFFF).toInt()
                    arm32RegisterTelemetry = "R0: $r0 | R1: $r1 | R2: $r2 | PC: 0x${Integer.toHexString(pc)}"
                    translationsCount += 2
                }

                // Wall collisions (Assuming ~400 width, 600 height dynamically measured)
                if (ballX < 12f) {
                    ballX = 12f
                    ballVx = abs(ballVx)
                }
                if (ballX > 380f) {
                    ballX = 380f
                    ballVx = -abs(ballVx)
                }
                if (ballY < 12f) {
                    ballY = 12f
                    ballVy = abs(ballVy)
                }

                // Paddle collision
                val paddleY = 520f
                if (ballY + 10f >= paddleY && ballY - 10f <= paddleY + 18f) {
                    if (ballX >= paddleX - 10f && ballX <= paddleX + paddleWidth + 10f) {
                        // Angle depends on where it hits the paddle
                        val hitOffset = (ballX - (paddleX + paddleWidth / 2f)) / (paddleWidth / 2f)
                        ballVx = hitOffset * 6.5f
                        ballVy = -abs(ballVy)
                        translationsCount += 4
                        Android9VirtualEnvironment.addLog("D", "libengine32.so", "paddle_bounce(hit=$hitOffset) -> JNI translation")
                    }
                }

                // Bottom boundary (Ball lost)
                if (ballY > 580f) {
                    lives--
                    Android9VirtualEnvironment.addLog("W", "Arcade32", "Life lost! Remaining: $lives")
                    if (lives <= 0) {
                        gameState = "GAMEOVER"
                    } else {
                        // Reset ball
                        ballX = paddleX + paddleWidth / 2f
                        ballY = paddleY - 20f
                        ballVx = 4.5f * if (Random.nextBoolean()) 1f else -1f
                        ballVy = -5.5f
                    }
                }

                // Brick collisions
                for (brick in bricks) {
                    if (!brick.isDestroyed) {
                        if (ballX >= brick.x && ballX <= brick.x + brick.width &&
                            ballY >= brick.y && ballY <= brick.y + brick.height
                        ) {
                            brick.isDestroyed = true
                            ballVy = -ballVy
                            score += brick.points
                            translationsCount += 8
                            Android9VirtualEnvironment.addLog("I", "Arcade32", "Brick destroyed! Score: $score")
                            break
                        }
                    }
                }

                // Check victory
                if (bricks.isNotEmpty() && bricks.all { it.isDestroyed }) {
                    gameState = "VICTORY"
                    Android9VirtualEnvironment.addLog("I", "Arcade32", "Level cleared! Victory!")
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C101A))
    ) {
        // --- Top Status Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161E2E))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RETRO ARCADE 32", color = Color(0xFFFFAB00), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("SKOR: $score", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(lives) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(13.dp))
                }
                Text("L$level", color = TechCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live JNI Register Translation Telemetry HUD
        Surface(
            color = Color(0xFF111827),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Houdini: $arm32RegisterTelemetry",
                    color = AccentGreen,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Çevrilen: $translationsCount",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- Main Game Canvas ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF090D14))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        paddleX = (paddleX + dragAmount.x).coerceIn(10f, 380f - paddleWidth)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        paddleX = (offset.x - paddleWidth / 2f).coerceIn(10f, 380f - paddleWidth)
                        if (gameState == "READY") restartGame()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // Lazily initialize brick coordinates if not yet initialized
                if (bricks.isEmpty() || bricks.first().width == 0f) {
                    initBricks(canvasW)
                }

                // Draw Bricks
                for (brick in bricks) {
                    if (!brick.isDestroyed) {
                        drawRect(
                            color = brick.color,
                            topLeft = Offset(brick.x, brick.y),
                            size = Size(brick.width, brick.height)
                        )
                        // Bevel border
                        drawRect(
                            color = Color.White.copy(alpha = 0.35f),
                            topLeft = Offset(brick.x, brick.y),
                            size = Size(brick.width, 3f)
                        )
                    }
                }

                // Draw Paddle (Neon Cyan)
                val paddleY = 520f
                drawRoundRect(
                    color = TechCyan,
                    topLeft = Offset(paddleX, paddleY),
                    size = Size(paddleWidth, 14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )

                // Draw Ball (White Glow)
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(ballX.coerceIn(8f, canvasW - 8f), ballY.coerceIn(8f, canvasH - 8f))
                )
                drawCircle(
                    color = TechCyan.copy(alpha = 0.3f),
                    radius = 12f,
                    center = Offset(ballX.coerceIn(8f, canvasW - 8f), ballY.coerceIn(8f, canvasH - 8f))
                )
            }

            // Start Overlay
            if (gameState == "READY") {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("RETRO BREAKOUT 32-BIT", color = Color(0xFFFFAB00), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Parmağınızı kaydırarak raketi yönlendirin", color = Color.LightGray, fontSize = 11.sp)
                        Button(
                            onClick = { restartGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OYUNU BAŞLAT", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (gameState == "GAMEOVER") {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.padding(20.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("OYUN BİTTİ", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Skorunuz: $score", color = Color.White, fontSize = 14.sp)
                            Button(onClick = { restartGame() }, colors = ButtonDefaults.buttonColors(containerColor = TechCyan)) {
                                Text("Yeniden Başla", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (gameState == "VICTORY") {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.padding(20.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("TEBRİKLER! BÖLÜM TEMİZLENDİ!", color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Skorunuz: $score", color = Color.White, fontSize = 14.sp)
                            Button(onClick = { restartGame() }, colors = ButtonDefaults.buttonColors(containerColor = TechCyan)) {
                                Text("Sonraki Seviye", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- On-Screen Interactive Direction Controls (Touch Buttons) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161E2E))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Left button
                Button(
                    onClick = { paddleX = (paddleX - 35f).coerceAtLeast(10f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C394E)),
                    modifier = Modifier.size(56.dp, 40.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Sola", tint = TechCyan)
                }

                // Right button
                Button(
                    onClick = { paddleX = (paddleX + 35f).coerceAtMost(380f - paddleWidth) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C394E)),
                    modifier = Modifier.size(56.dp, 40.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Sağa", tint = TechCyan)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { restartGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sıfırla", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
