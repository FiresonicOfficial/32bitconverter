package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.Arm32Emulator
import com.example.model.ArmRegisterState
import com.example.model.DisassembledInstruction
import com.example.ui.theme.*

@Composable
fun TranslationSandboxView(
    modifier: Modifier = Modifier
) {
    val emulator = remember { Arm32Emulator() }
    var selectedPreset by remember { mutableStateOf(Arm32Emulator.PresetProgram.HELLO_SYSCALL) }
    var registerState by remember { mutableStateOf(emulator.state) }
    var pcIndex by remember { mutableIntStateOf(emulator.programCounterIndex) }
    var consoleLogs by remember { mutableStateOf(emulator.consoleOutput.toList()) }
    val program = emulator.currentProgram

    fun syncState() {
        registerState = emulator.state
        pcIndex = emulator.programCounterIndex
        consoleLogs = emulator.consoleOutput.toList()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "ARM32 -> ARM64 İKİLİ ÇEVİRİ VE EMÜLASYON",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TechCyan,
            letterSpacing = 1.sp
        )

        // Preset Program Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Test Programı Seçin:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Arm32Emulator.PresetProgram.values().take(2).forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                emulator.loadPresetProgram(preset)
                                syncState()
                            },
                            label = { Text(preset.title.take(18), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Arm32Emulator.PresetProgram.values().drop(2).forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                emulator.loadPresetProgram(preset)
                                syncState()
                            },
                            label = { Text(preset.title.take(18), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = selectedPreset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Control Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    emulator.step()
                    syncState()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black),
                modifier = Modifier.weight(1f).testTag("emulator_step_button")
            ) {
                Icon(Icons.Default.Redo, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Adım", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    emulator.runAll()
                    syncState()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                modifier = Modifier.weight(1f).testTag("emulator_run_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Çalıştır", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    emulator.reset()
                    emulator.loadPresetProgram(selectedPreset)
                    syncState()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("emulator_reset_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sıfırla")
            }
        }

        // Instruction Disassembly & Translation Table
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("instruction_table_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("32-Bit ARM (Giriş)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
                    Text("-> Çevrilen 64-Bit AArch64", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TechCyan)
                }

                program.forEachIndexed { index, instr ->
                    val isCurrent = index == pcIndex
                    val isPast = index < pcIndex
                    InstructionRowItem(instr = instr, isCurrent = isCurrent, isPast = isPast)
                }
            }
        }

        // CPU Register State View
        Text(
            text = "YAZMAÇ (REGISTER) VE BAYRAK DURUMU",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TechCyan,
            letterSpacing = 1.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("registers_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Registers Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    RegBox("R0/W0", registerState.r0)
                    RegBox("R1/W1", registerState.r1)
                    RegBox("R2/W2", registerState.r2)
                    RegBox("R3/W3", registerState.r3)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    RegBox("R7/X8", registerState.r7)
                    RegBox("SP (R13)", registerState.sp)
                    RegBox("LR (R14)", registerState.lr)
                    RegBox("PC (R15)", registerState.pc)
                }

                // CPSR Flags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CPSR Bayrakları:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlagIndicator("Z (Zero)", registerState.flagZ)
                        FlagIndicator("N (Neg)", registerState.flagN)
                        FlagIndicator("C (Carry)", registerState.flagC)
                        FlagIndicator("V (Ovf)", registerState.flagV)
                    }
                }
            }
        }

        // Output Terminal Console
        Text(
            text = "KONSOL & ÇEVİRİ GÜNLÜĞÜ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TechCyan,
            letterSpacing = 1.sp
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070D18)),
            modifier = Modifier.fillMaxWidth().testTag("console_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (consoleLogs.isEmpty()) {
                    Text(
                        text = "$ [Çevirici Beklemede] 'Adım' veya 'Çalıştır' butonuna basın.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                } else {
                    consoleLogs.forEach { log ->
                        val color = if (log.contains("KONSOL ÇIKTISI")) AccentGreen else TechCyan
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionRowItem(
    instr: DisassembledInstruction,
    isCurrent: Boolean,
    isPast: Boolean
) {
    val bg = when {
        isCurrent -> TechCyan.copy(alpha = 0.18f)
        isPast -> DarkSurfaceVariant.copy(alpha = 0.5f)
        else -> DarkSurfaceVariant
    }

    val border = if (isCurrent) Modifier.border(1.dp, TechCyan, RoundedCornerShape(8.dp)) else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(border)
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isCurrent) {
                    Icon(Icons.Default.ArrowRight, contentDescription = null, tint = TechCyan, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${instr.arm32Mnemonic} ${instr.arm32Operands}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) AccentAmber else MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${instr.translatedArm64Mnemonic} ${instr.translatedArm64Operands}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = instr.explanation,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun RegBox(label: String, value: Int) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = DarkSurfaceVariant,
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "0x${Integer.toHexString(value).uppercase()}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TechCyan
            )
        }
    }
}

@Composable
private fun FlagIndicator(name: String, isActive: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isActive) AccentGreen.copy(alpha = 0.2f) else Color.Transparent
    ) {
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) AccentGreen else MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
