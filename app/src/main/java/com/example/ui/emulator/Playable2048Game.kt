package com.example.ui.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.Android9VirtualEnvironment
import com.example.model.Android9VirtualApp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.TechCyan
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun Playable2048Game(
    app: Android9VirtualApp,
    modifier: Modifier = Modifier
) {
    var board by remember { mutableStateOf(Array(4) { IntArray(4) }) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(1024) }
    var gameOver by remember { mutableStateOf(false) }

    fun addRandomTile() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0..3) {
            for (c in 0..3) {
                if (board[r][c] == 0) emptyCells.add(Pair(r, c))
            }
        }
        if (emptyCells.isNotEmpty()) {
            val (r, c) = emptyCells.random()
            board[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
        }
    }

    fun restart() {
        board = Array(4) { IntArray(4) }
        score = 0
        gameOver = false
        addRandomTile()
        addRandomTile()
        Android9VirtualEnvironment.addLog("I", "2048", "New 2048 session started. ARMv7 logic initialized.")
    }

    LaunchedEffect(Unit) {
        if (board.all { row -> row.all { it == 0 } }) {
            restart()
        }
    }

    fun moveLeft(): Boolean {
        var moved = false
        val newBoard = Array(4) { IntArray(4) }
        var addedScore = 0

        for (r in 0..3) {
            val nonZeros = board[r].filter { it != 0 }.toMutableList()
            val merged = mutableListOf<Int>()
            var i = 0
            while (i < nonZeros.size) {
                if (i + 1 < nonZeros.size && nonZeros[i] == nonZeros[i + 1]) {
                    val doubled = nonZeros[i] * 2
                    merged.add(doubled)
                    addedScore += doubled
                    i += 2
                    moved = true
                } else {
                    merged.add(nonZeros[i])
                    i++
                }
            }
            for (c in 0..3) {
                if (c < merged.size) {
                    newBoard[r][c] = merged[c]
                    if (newBoard[r][c] != board[r][c]) moved = true
                }
            }
        }

        if (moved) {
            board = newBoard
            score += addedScore
            if (score > highScore) highScore = score
            addRandomTile()
            Android9VirtualEnvironment.addLog("D", "2048", "Move LEFT executed. Score: $score")
        }
        return moved
    }

    fun moveRight(): Boolean {
        // Reverse rows, move left, reverse back
        for (r in 0..3) board[r].reverse()
        val moved = moveLeft()
        for (r in 0..3) board[r].reverse()
        return moved
    }

    fun moveUp(): Boolean {
        // Transpose, move left, transpose back
        val transposed = Array(4) { r -> IntArray(4) { c -> board[c][r] } }
        board = transposed
        val moved = moveLeft()
        val back = Array(4) { r -> IntArray(4) { c -> board[c][r] } }
        board = back
        return moved
    }

    fun moveDown(): Boolean {
        val transposed = Array(4) { r -> IntArray(4) { c -> board[c][r] } }
        board = transposed
        val moved = moveRight()
        val back = Array(4) { r -> IntArray(4) { c -> board[c][r] } }
        board = back
        return moved
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("2048 PUZZLE", color = AccentAmber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Kareleri kaydırıp birleştirin!", color = Color.Gray, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SKOR", fontSize = 9.sp, color = Color.Gray)
                        Text("$score", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EN İYİ", fontSize = 9.sp, color = Color.Gray)
                        Text("$highScore", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TechCyan)
                    }
                }
            }
        }

        // 4x4 Grid Board with Swipe detection
        var dragTotalX by remember { mutableFloatStateOf(0f) }
        var dragTotalY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .size(310.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragTotalX += dragAmount.x
                            dragTotalY += dragAmount.y
                        },
                        onDragEnd = {
                            if (abs(dragTotalX) > abs(dragTotalY)) {
                                if (dragTotalX > 30f) moveRight()
                                else if (dragTotalX < -30f) moveLeft()
                            } else {
                                if (dragTotalY > 30f) moveDown()
                                else if (dragTotalY < -30f) moveUp()
                            }
                            dragTotalX = 0f
                            dragTotalY = 0f
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (r in 0..3) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0..3) {
                            val value = board[r][c]
                            val tileColor = when (value) {
                                2 -> Color(0xFF2C3E50)
                                4 -> Color(0xFF34495E)
                                8 -> Color(0xFFE67E22)
                                16 -> Color(0xFFD35400)
                                32 -> Color(0xFFE74C3C)
                                64 -> Color(0xFFC0392B)
                                128 -> Color(0xFFF1C40F)
                                256 -> Color(0xFFF39C12)
                                512 -> Color(0xFF2ECC71)
                                1024 -> Color(0xFF1ABC9C)
                                2048 -> Color(0xFF9B59B6)
                                else -> Color(0xFF151E2E)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tileColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (value > 0) {
                                    Text(
                                        text = "$value",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (value >= 1024) 16.sp else 20.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // On-Screen Direction Pad Buttons (for precision gameplay)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { moveUp() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF243044))
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Yukarı", tint = TechCyan)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                IconButton(
                    onClick = { moveLeft() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF243044))
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Sola", tint = TechCyan)
                }
                IconButton(
                    onClick = { restart() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = AccentAmber)
                }
                IconButton(
                    onClick = { moveRight() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF243044))
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sağa", tint = TechCyan)
                }
            }
            IconButton(
                onClick = { moveDown() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF243044))
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Aşağı", tint = TechCyan)
            }
        }
    }
}
