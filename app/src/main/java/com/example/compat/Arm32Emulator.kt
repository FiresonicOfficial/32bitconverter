package com.example.compat

import com.example.model.ArmRegisterState
import com.example.model.DisassembledInstruction

class Arm32Emulator {

    var state = ArmRegisterState()
        private set

    val memory = ByteArray(65536) // 64KB virtual memory sandbox
    val consoleOutput = mutableListOf<String>()
    var programCounterIndex = 0
        private set

    var currentProgram: List<DisassembledInstruction> = emptyList()
        private set

    init {
        loadPresetProgram(PresetProgram.HELLO_SYSCALL)
    }

    enum class PresetProgram(val title: String, val description: String) {
        HELLO_SYSCALL(
            "ARM32 Syscall Çevirisi (write)",
            "AArch32'deki R7=4 (sys_write) çağrısını AArch64 X8=64 sistem çağrısına çevirir ve konsola yazar."
        ),
        FIBONACCI_LOOP(
            "Fibonacci Döngüsü (Koşullu Dallanma)",
            "R0 ve R1 üzerinden CMP, BNE ve ADD döngüsünü 64-bit register eşlemesiyle simüle eder."
        ),
        POINTER_WIDENING(
            "İşaretçi Genişletme (Pointer Widening)",
            "32-bit bellek adreslerini (0x7FFFF000) 64-bit adres uzayına sıfır uzatmalı (zero-extend) olarak haritalar."
        ),
        BITWISE_LOGIC(
            "Mantıksal ve Aritmetik İşlemler",
            "AND, ORR, EOR, LSL ve ROR işlemlerinin 32-bit'ten 64-bit'e çevrim mantığını test eder."
        )
    }

    fun loadPresetProgram(preset: PresetProgram) {
        consoleOutput.clear()
        state = ArmRegisterState()
        programCounterIndex = 0

        currentProgram = when (preset) {
            PresetProgram.HELLO_SYSCALL -> {
                // Initialize "ARM32 OK\n" string in memory
                val text = "ARM32->ARM64 KÖPRÜ ÇALIŞIYOR!\n"
                val textBytes = text.toByteArray(Charsets.UTF_8)
                val strAddr = 0x2000
                System.arraycopy(textBytes, 0, memory, strAddr, textBytes.size)

                listOf(
                    DisassembledInstruction(
                        address = 0x10000,
                        rawHex32 = "0xE3A00001",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R0, #1",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "W0, #1",
                        explanation = "Standart çıktı (stdout fd=1) dosya tanıtıcısını R0/W0'a yükler."
                    ),
                    DisassembledInstruction(
                        address = 0x10004,
                        rawHex32 = "0xE3A01C20",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R1, #0x2000",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "X1, #0x2000",
                        explanation = "32-bit metin işaretçisi 64-bit X1 register'ına sıfır uzatmalı (zero-extended) taşınır."
                    ),
                    DisassembledInstruction(
                        address = 0x10008,
                        rawHex32 = "0xE3A0201E",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R2, #30",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "X2, #30",
                        explanation = "Metin uzunluğu (30 bayt) R2/X2'ye yüklenir."
                    ),
                    DisassembledInstruction(
                        address = 0x1000C,
                        rawHex32 = "0xE3A07004",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R7, #4",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "X8, #64",
                        explanation = "[KRİTİK ÇEVİRİ] ARM32 sys_write çağrı numarası 4, ARM64'te 64 (sys_write) numarasına çevrilir!"
                    ),
                    DisassembledInstruction(
                        address = 0x10010,
                        rawHex32 = "0xEF000000",
                        arm32Mnemonic = "SVC",
                        arm32Operands = "#0",
                        translatedArm64Mnemonic = "SVC",
                        translatedArm64Operands = "#0",
                        explanation = "Çekirdek sistem çağrısı tetiklenir. Çıktı köprü üzerinden işletim sistemine iletilir."
                    )
                )
            }
            PresetProgram.FIBONACCI_LOOP -> {
                listOf(
                    DisassembledInstruction(
                        address = 0x10000,
                        rawHex32 = "0xE3A00000",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R0, #0",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "W0, #0",
                        explanation = "Fibonacci ilk terim F(0) = 0 R0'a atanır."
                    ),
                    DisassembledInstruction(
                        address = 0x10004,
                        rawHex32 = "0xE3A01001",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R1, #1",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "W1, #1",
                        explanation = "Fibonacci ikinci terim F(1) = 1 R1'e atanır."
                    ),
                    DisassembledInstruction(
                        address = 0x10008,
                        rawHex32 = "0xE3A02008",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R2, #8",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "W2, #8",
                        explanation = "Döngü sayacı (8 adım) R2'ye atanır."
                    ),
                    DisassembledInstruction(
                        address = 0x1000C,
                        rawHex32 = "0xE0803001",
                        arm32Mnemonic = "ADD",
                        arm32Operands = "R3, R0, R1",
                        translatedArm64Mnemonic = "ADD",
                        translatedArm64Operands = "W3, W0, W1",
                        explanation = "R3 = R0 + R1 (Yeni Fibonacci değeri)."
                    ),
                    DisassembledInstruction(
                        address = 0x10010,
                        rawHex32 = "0xE1A00001",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R0, R1",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "W0, W1",
                        explanation = "R0 = R1 terim kaydırma."
                    ),
                    DisassembledInstruction(
                        address = 0x10014,
                        rawHex32 = "0xE1A01003",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R1, R3",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "W1, W3",
                        explanation = "R1 = R3 terim güncelleme."
                    ),
                    DisassembledInstruction(
                        address = 0x10018,
                        rawHex32 = "0xE2422001",
                        arm32Mnemonic = "SUB",
                        arm32Operands = "R2, R2, #1",
                        translatedArm64Mnemonic = "SUB",
                        translatedArm64Operands = "W2, W2, #1",
                        explanation = "Döngü sayacı 1 azaltılır (R2 = R2 - 1)."
                    ),
                    DisassembledInstruction(
                        address = 0x1001C,
                        rawHex32 = "0xE3520000",
                        arm32Mnemonic = "CMP",
                        arm32Operands = "R2, #0",
                        translatedArm64Mnemonic = "CMP",
                        translatedArm64Operands = "W2, #0",
                        explanation = "Sayacın 0 olup olmadığı kontrol edilir (Z bayrağı güncellenir)."
                    )
                )
            }
            PresetProgram.POINTER_WIDENING -> {
                listOf(
                    DisassembledInstruction(
                        address = 0x10000,
                        rawHex32 = "0xE3A00801",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R0, #0x1000",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "X0, #0x1000",
                        explanation = "32-bit bellek adresi 64-bit X0 taban yazmacına yüklenir."
                    ),
                    DisassembledInstruction(
                        address = 0x10004,
                        rawHex32 = "0xE3A0102A",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R1, #42",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "W1, #42",
                        explanation = "Saklanacak 32-bit veri (42) R1/W1'e yüklenir."
                    ),
                    DisassembledInstruction(
                        address = 0x10008,
                        rawHex32 = "0xE5801000",
                        arm32Mnemonic = "STR",
                        arm32Operands = "R1, [R0]",
                        translatedArm64Mnemonic = "STR",
                        translatedArm64Operands = "W1, [X0]",
                        explanation = "[BELLEK KÖPRÜSÜ] 32-bit değer 64-bit adresle [X0] konumuna yazılır. Adres aşımı önlenir."
                    ),
                    DisassembledInstruction(
                        address = 0x1000C,
                        rawHex32 = "0xE5902000",
                        arm32Mnemonic = "LDR",
                        arm32Operands = "R2, [R0]",
                        translatedArm64Mnemonic = "LDR",
                        translatedArm64Operands = "W2, [X0]",
                        explanation = "Yazılan değer [X0] adresinden R2/W2'ye okunur."
                    )
                )
            }
            PresetProgram.BITWISE_LOGIC -> {
                listOf(
                    DisassembledInstruction(
                        address = 0x10000,
                        rawHex32 = "0xE3A000FF",
                        arm32Mnemonic = "MOV",
                        arm32Operands = "R0, #0xFF",
                        translatedArm64Mnemonic = "MOV",
                        translatedArm64Operands = "W0, #0xFF",
                        explanation = "R0 = 0x000000FF"
                    ),
                    DisassembledInstruction(
                        address = 0x10004,
                        rawHex32 = "0xE1A01100",
                        arm32Mnemonic = "LSL",
                        arm32Operands = "R1, R0, #2",
                        translatedArm64Mnemonic = "LSL",
                        translatedArm64Operands = "W1, W0, #2",
                        explanation = "R1 = R0 << 2 (0x3FC) sola mantıksal kaydırma."
                    ),
                    DisassembledInstruction(
                        address = 0x10008,
                        rawHex32 = "0xE0202001",
                        arm32Mnemonic = "EOR",
                        arm32Operands = "R2, R0, R1",
                        translatedArm64Mnemonic = "EOR",
                        translatedArm64Operands = "W2, W0, W1",
                        explanation = "R2 = R0 XOR R1 (Bitwise özel veya işlemi)."
                    )
                )
            }
        }
    }

    fun step(): Boolean {
        if (programCounterIndex >= currentProgram.size) return false

        val instr = currentProgram[programCounterIndex]
        executeInstruction(instr)
        programCounterIndex++
        return programCounterIndex < currentProgram.size
    }

    fun runAll() {
        while (step()) {
            // execute to completion
        }
    }

    fun reset() {
        state = ArmRegisterState()
        programCounterIndex = 0
        consoleOutput.clear()
    }

    private fun executeInstruction(instr: DisassembledInstruction) {
        val op = instr.arm32Mnemonic
        val operands = instr.arm32Operands

        when (op) {
            "MOV" -> {
                // e.g. R0, #1 or R1, #0x2000 or R0, R1
                val parts = operands.split(",").map { it.trim() }
                if (parts.size == 2) {
                    val destReg = parts[0]
                    val src = parts[1]
                    val value = parseValue(src)
                    setRegister(destReg, value)
                    consoleOutput.add("[EXEC] $destReg = 0x${Integer.toHexString(value).uppercase()} ($value)")
                }
            }
            "ADD" -> {
                val parts = operands.split(",").map { it.trim() }
                if (parts.size == 3) {
                    val dest = parts[0]
                    val v1 = getRegister(parts[1])
                    val v2 = parseValue(parts[2])
                    val res = v1 + v2
                    setRegister(dest, res)
                    updateFlags(res)
                    consoleOutput.add("[EXEC] $dest = $v1 + $v2 -> $res")
                }
            }
            "SUB" -> {
                val parts = operands.split(",").map { it.trim() }
                if (parts.size == 3) {
                    val dest = parts[0]
                    val v1 = getRegister(parts[1])
                    val v2 = parseValue(parts[2])
                    val res = v1 - v2
                    setRegister(dest, res)
                    updateFlags(res)
                    consoleOutput.add("[EXEC] $dest = $v1 - $v2 -> $res")
                }
            }
            "CMP" -> {
                val parts = operands.split(",").map { it.trim() }
                if (parts.size == 2) {
                    val v1 = getRegister(parts[0])
                    val v2 = parseValue(parts[1])
                    val diff = v1 - v2
                    updateFlags(diff)
                    consoleOutput.add("[EXEC] CMP $v1 ile $v2 karşılaştırıldı -> ZeroFlag: ${diff == 0}")
                }
            }
            "STR" -> {
                // STR R1, [R0]
                val regVal = getRegister("R1")
                val addr = getRegister("R0") and 0xFFFF
                memory[addr] = (regVal and 0xFF).toByte()
                memory[addr + 1] = ((regVal shr 8) and 0xFF).toByte()
                memory[addr + 2] = ((regVal shr 16) and 0xFF).toByte()
                memory[addr + 3] = ((regVal shr 24) and 0xFF).toByte()
                consoleOutput.add("[EXEC MEM] 0x${Integer.toHexString(addr).uppercase()} adresine 32-bit $regVal yazıldı.")
            }
            "LDR" -> {
                // LDR R2, [R0]
                val addr = getRegister("R0") and 0xFFFF
                val b0 = memory[addr].toInt() and 0xFF
                val b1 = memory[addr + 1].toInt() and 0xFF
                val b2 = memory[addr + 2].toInt() and 0xFF
                val b3 = memory[addr + 3].toInt() and 0xFF
                val loaded = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
                setRegister("R2", loaded)
                consoleOutput.add("[EXEC MEM] 0x${Integer.toHexString(addr).uppercase()} adresinden R2'ye okundu -> $loaded")
            }
            "LSL" -> {
                val parts = operands.split(",").map { it.trim() }
                if (parts.size == 3) {
                    val dest = parts[0]
                    val srcVal = getRegister(parts[1])
                    val shift = parseValue(parts[2])
                    val res = srcVal shl shift
                    setRegister(dest, res)
                    consoleOutput.add("[EXEC] $dest = $srcVal << $shift -> $res")
                }
            }
            "EOR" -> {
                val parts = operands.split(",").map { it.trim() }
                if (parts.size == 3) {
                    val dest = parts[0]
                    val v1 = getRegister(parts[1])
                    val v2 = getRegister(parts[2])
                    val res = v1 xor v2
                    setRegister(dest, res)
                    consoleOutput.add("[EXEC] $dest = $v1 XOR $v2 -> $res")
                }
            }
            "SVC" -> {
                // Syscall dispatch: R7 contains syscall number in ARM32
                val syscallNr = state.r7
                if (syscallNr == 4) { // sys_write
                    val fd = state.r0
                    val bufPtr = state.r1 and 0xFFFF
                    val count = state.r2
                    val sb = StringBuilder()
                    for (i in 0 until count.coerceAtMost(memory.size - bufPtr)) {
                        val c = memory[bufPtr + i].toInt().toChar()
                        if (c != '\u0000') sb.append(c)
                    }
                    val outText = sb.toString().trim()
                    consoleOutput.add(">>> [KONSOL ÇIKTISI (fd=$fd)]: \"$outText\"")
                    setRegister("R0", count) // Return bytes written
                } else {
                    consoleOutput.add("[SYSCALL] Bilinmeyen çağrı no: $syscallNr")
                }
            }
        }

        // Update PC
        state = state.copy(pc = instr.address + 4)
    }

    private fun parseValue(str: String): Int {
        val trimmed = str.trim()
        return when {
            trimmed.startsWith("#0x") || trimmed.startsWith("#0X") -> {
                trimmed.substring(3).toIntOrNull(16) ?: 0
            }
            trimmed.startsWith("0x") || trimmed.startsWith("0X") -> {
                trimmed.substring(2).toIntOrNull(16) ?: 0
            }
            trimmed.startsWith("#") -> {
                trimmed.substring(1).toIntOrNull() ?: 0
            }
            trimmed.startsWith("R") || trimmed.startsWith("r") -> {
                getRegister(trimmed)
            }
            else -> trimmed.toIntOrNull() ?: 0
        }
    }

    private fun getRegister(name: String): Int {
        return when (name.uppercase()) {
            "R0" -> state.r0
            "R1" -> state.r1
            "R2" -> state.r2
            "R3" -> state.r3
            "R4" -> state.r4
            "R5" -> state.r5
            "R6" -> state.r6
            "R7" -> state.r7
            "R8" -> state.r8
            "R9" -> state.r9
            "R10" -> state.r10
            "R11" -> state.r11
            "R12" -> state.r12
            "SP", "R13" -> state.sp
            "LR", "R14" -> state.lr
            "PC", "R15" -> state.pc
            else -> 0
        }
    }

    private fun setRegister(name: String, value: Int) {
        state = when (name.uppercase()) {
            "R0" -> state.copy(r0 = value)
            "R1" -> state.copy(r1 = value)
            "R2" -> state.copy(r2 = value)
            "R3" -> state.copy(r3 = value)
            "R4" -> state.copy(r4 = value)
            "R5" -> state.copy(r5 = value)
            "R6" -> state.copy(r6 = value)
            "R7" -> state.copy(r7 = value)
            "R8" -> state.copy(r8 = value)
            "R9" -> state.copy(r9 = value)
            "R10" -> state.copy(r10 = value)
            "R11" -> state.copy(r11 = value)
            "R12" -> state.copy(r12 = value)
            "SP", "R13" -> state.copy(sp = value)
            "LR", "R14" -> state.copy(lr = value)
            "PC", "R15" -> state.copy(pc = value)
            else -> state
        }
    }

    private fun updateFlags(result: Int) {
        state = state.copy(
            flagZ = result == 0,
            flagN = result < 0
        )
    }
}
