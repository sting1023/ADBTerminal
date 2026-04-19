package com.sting.adbterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ADBTerminalUI()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ADBTerminalUI() {
    val scope = rememberCoroutineScope()
    var commandText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf(">>> ADB Terminal Ready\n> 输入命令后点击执行\n") }
    var isRunning by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // 自动滚动
    LaunchedEffect(outputText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "ADB Terminal",
            color = Color(0xFF00FF00),
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 输出区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = outputText,
                color = Color(0xFF00FF00),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 命令输入框
        OutlinedTextField(
            value = commandText,
            onValueChange = { commandText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "输入 ADB 命令，如: pm list packages",
                    color = Color.Gray
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FF00),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color(0xFF00FF00)
            ),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 执行按钮
            Button(
                onClick = {
                    if (commandText.isNotBlank() && !isRunning) {
                        scope.launch {
                            isRunning = true
                            val result = executeCommand(commandText.trim())
                            outputText += "\n\$ ${commandText.trim()}\n$result\n"
                            if (outputText.length > 50000) {
                                outputText = outputText.takeLast(30000)
                            }
                            commandText = ""
                            isRunning = false
                        }
                    }
                },
                enabled = !isRunning && commandText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00AA00),
                    disabledContainerColor = Color(0xFF005500)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isRunning) "执行中..." else "执行", color = Color.White)
            }

            // 清空输出
            Button(
                onClick = { outputText = ">>> 已清空\n" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                modifier = Modifier.weight(1f)
            ) {
                Text("清空输出", color = Color.White)
            }

            // 清空输入
            Button(
                onClick = { commandText = "" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                modifier = Modifier.weight(1f)
            ) {
                Text("清空输入", color = Color.White)
            }
        }
    }
}

suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
    try {
        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        val output = StringBuilder()
        val errorOutput = StringBuilder()

        // 读取标准输出
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        // 读取错误输出
        while (errorReader.readLine().also { line = it } != null) {
            errorOutput.append(line).append("\n")
        }

        process.waitFor()
        reader.close()
        errorReader.close()

        val result = output.toString()
        val error = errorOutput.toString()

        if (error.isNotBlank()) {
            "[stderr]\n$error${if (result.isNotBlank()) "\n[stdout]\n$result" else ""}"
        } else {
            result.ifBlank { "[命令执行完成，无输出]" }
        }
    } catch (e: Exception) {
        "[Error] ${e.message}"
    }
}
