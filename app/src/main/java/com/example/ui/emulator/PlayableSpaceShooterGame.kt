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
import kotlin.random.Random

data class LaserBeam(var x: Float, var y: Float, val speed: Float = 14f)
data class SpaceEnemy(var x: Float, var y: Float, val size: Float, val speed: Float, var health: Int)

@Composable
fun PlayableSpaceShooterGame(
    app: Android9VirtualApp,
    modifier: Modifier = Modifier
) {
    var gameState by remember { mutableStateOf("READY") } // READY, RUNNING, GAMEOVER
    var score by remember { mutableIntStateOf(0) }
    var shield by remember { mutableIntStateOf(100) }
    var wave by remember { mutableIntStateOf(1) }

    var shipX by remember { mutableFloatStateOf(180f) }
    var shipY by remember { mutableFloatStateOf(520f) }

    val lasers = remember { mutableStateListOf<LaserBeam>() }
    val enemies = remember { mutableStateListOf<SpaceEnemy>() }

    var framesRendered by remember { mutableLongStateOf(0L) }
    var jniTranslationsCount by remember { mutableLongStateOf(0L) }

    fun restartGame() {
        score = 0
        shield = 100
        wave = 1
        shipX = 180f
        shipY = 520f
        lasers.clear()
        enemies.clear()
        gameState = "RUNNING"
        Android9VirtualEnvironment.addLog("I", "SpaceShooter", "ARMv7 32-bit Space Combat Engine initialized.")
    }

    fun fireLaser() {
        if (gameState == "RUNNING") {
            lasers.add(LaserBeam(x = shipX + 16f, y = shipY - 10f))
            jniTranslationsCount += 2
            Android9VirtualEnvironment.addLog("D", "libengine32.so", "laser_fire(x=${shipX.toInt()}) [ARM32 JNI]")
        } else if (gameState == "READY") {
            restartGame()
        }
    }

    // 60 FPS Game Loop
    LaunchedEffect(gameState) {
        if (gameState == "RUNNING") {
            while (isActive && gameState == "RUNNING") {
                delay(16L) // ~60 FPS
                framesRendered++

                // Spawn enemies periodically
                if (framesRendered % 45 == 0L) {
                    val ex = Random.nextFloat() * 320f + 20f
                    val esize = Random.nextFloat() * 18f + 16f
                    val espeed = Random.nextFloat() * 2.5f + 2.0f
                    enemies.add(SpaceEnemy(x = ex, y = -30f, size = esize, speed = espeed, health = 2))
                }

                // Move lasers
                for (i in lasers.indices.reversed()) {
                    val l = lasers[i]
                    l.y -= l.speed
                    if (l.y < -20f) {
                        lasers.removeAt(i)
                    }
                }

                // Move enemies & collision with ship / lasers
                for (ei in enemies.indices.reversed()) {
                    val e = enemies[ei]
                    e.y += e.speed

                    // Hit bottom
                    if (e.y > 640f) {
                        enemies.removeAt(ei)
                        continue
                    }

                    // Collision with laser
                    var destroyed = false
                    for (li in lasers.indices.reversed()) {
                        val l = lasers[li]
                        val dist = (l.x - e.x) * (l.x - e.x) + (l.y - e.y) * (l.y - e.y)
                        if (dist < (e.size + 10f) * (e.size + 10f)) {
                            lasers.removeAt(li)
                            e.health--
                            if (e.health <= 0) {
                                enemies.removeAt(ei)
                                score += 25
                                jniTranslationsCount += 6
                                destroyed = true
                                break
                            }
                        }
                    }

                    if (destroyed) continue

                    // Collision with ship
                    val distToShip = (shipX + 16f - e.x) * (shipX + 16f - e.x) + (shipY + 16f - e.y) * (shipY + 16f - e.y)
                    if (distToShip < (e.size + 18f) * (e.size + 18f)) {
                        enemies.removeAt(ei)
                        shield -= 25
                        Android9VirtualEnvironment.addLog("W", "SpaceShooter", "Ship damaged! Shield: $shield%")
                        if (shield <= 0) {
                            gameState = "GAMEOVER"
                            Android9VirtualEnvironment.addLog("E", "SpaceShooter", "Ship destroyed. Game Over.")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        // --- Top Status Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SPACE COMBAT 32-BIT", color = TechCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("SKOR: $score", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("KALKAN: $shield%", color = if (shield > 40) AccentGreen else Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("DALGA $wave", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // --- Live Playable Space Canvas ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF02040A))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        shipX = (shipX + dragAmount.x).coerceIn(10f, 350f)
                        shipY = (shipY + dragAmount.y).coerceIn(150f, 560f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        fireLaser()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height

                // Starfield background
                for (s in 0..30) {
                    val sx = (s * 87f + framesRendered * 1.5f) % cw
                    val sy = (s * 53f + framesRendered * 2.5f) % ch
                    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 1.5f, center = Offset(sx, sy))
                }

                // Render Lasers (Neon Cyan)
                for (l in lasers) {
                    drawRoundRect(
                        color = TechCyan,
                        topLeft = Offset(l.x - 2f, l.y),
                        size = Size(4f, 16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                }

                // Render Enemies (Orange & Crimson Alien Ships / Asteroids)
                for (e in enemies) {
                    drawCircle(
                        color = Color(0xFFFF3D00),
                        radius = e.size,
                        center = Offset(e.x, e.y)
                    )
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = e.size * 0.5f,
                        center = Offset(e.x, e.y)
                    )
                }

                // Render Spaceship (Sleek Sci-Fi Triangle)
                val p1 = Offset(shipX + 16f, shipY)
                val p2 = Offset(shipX, shipY + 36f)
                val p3 = Offset(shipX + 32f, shipY + 36f)
                val shipPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }
                drawPath(shipPath, color = TechCyan)

                // Engine Thruster Glow
                drawCircle(
                    color = Color(0xFFFF9100),
                    radius = 6f + (framesRendered % 4),
                    center = Offset(shipX + 16f, shipY + 40f)
                )
            }

            if (gameState == "READY") {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("SPACE COMBAT 32-BIT", color = TechCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Gemiyi parmağınızla sürükleyin, ekrana dokunarak ateş edin!", color = Color.White, fontSize = 11.sp)
                        Button(onClick = { restartGame() }, colors = ButtonDefaults.buttonColors(containerColor = TechCyan)) {
                            Text("GÖREVE BAŞLA", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (gameState == "GAMEOVER") {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.padding(20.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("GEMİ İMHA EDİLDİ", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Nihai Skor: $score", color = Color.White, fontSize = 14.sp)
                            Button(onClick = { restartGame() }, colors = ButtonDefaults.buttonColors(containerColor = TechCyan)) {
                                Text("Yeniden Başla", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Interactive Controller Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { fireLaser() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_space_fire")
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ATEŞ ET (LAZER)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { restartGame() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Yeniden Başlat", tint = TechCyan)
            }
        }
    }
}
