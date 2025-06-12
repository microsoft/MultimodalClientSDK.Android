package com.example.sampleapplication

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sampleapplication.ui.theme.SampleApplicationTheme
import com.microsoft.multimodal.clientsdk.MultimodalClientSDK
import com.microsoft.multimodal.clientsdk.configs.SDKConfigs
import com.microsoft.multimodal.clientsdk.models.ChatMessage
import com.microsoft.multimodal.clientsdk.models.MessageResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity :
    AppCompatActivity() { // need to replace with AppCompatActivity to use supportFragmentManager or other option is to ask client to pass supportFragmentManager which is required to build adaptive card

    private var isVoiceRecording by mutableStateOf(false)
    private var userToken by mutableStateOf("")
    private var enableSpeech by mutableStateOf(false)
    private var isProceedClicked by mutableStateOf(false)

    private val token = "" // Token Endpoint Of Bot
    private val speechSubscriptionKey = "" // Speech Subscription Key
    private val speechServiceRegion = "" // Speech Service Region

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            if (!isProceedClicked) {
                SetupScreen(
                    token = userToken,
                    onTokenChange = { userToken = it },
                    enableSpeech = enableSpeech,
                    onEnableSpeechChange = { enableSpeech = it },
                    onProceed = {
                        val finalToken =
                            if (userToken.contains("/directline/token?api-version")) userToken else token
                        val speechSubscriptionKey = if (enableSpeech) speechSubscriptionKey else ""
                        val speechServiceRegion = if (enableSpeech) speechServiceRegion else ""
                        val authConfigs = mapOf(
                            "isAnonymousUser" to true // or false
                        )

                        val sdkConfigs = SDKConfigs(
                            tokenUrl = finalToken,
                            speechSubscriptionKey = speechSubscriptionKey,
                            speechServiceRegion = speechServiceRegion,
                            authConfigs = authConfigs
                        )

                        MultimodalClientSDK.init(
                            this@MainActivity,
                            sdkConfigs
                        )
                        isProceedClicked = true
                    }
                )
            } else {
                MainScreen(
                    isVoiceRecording = isVoiceRecording,
                    onRecordClick = { handleRecordClick() }
                )
            }
        }
        requestPermissions.launch(
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET)
        )
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.RECORD_AUDIO] == true &&
                permissions[Manifest.permission.INTERNET] == true
            ) {
                Log.e("Permissions", "Permissions granted")
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

    fun handleRecordClick() {
        if (isVoiceRecording) {
            MultimodalClientSDK.sdk?.stopContinuousListening()
        } else {
            MultimodalClientSDK.sdk?.startContinuousListening()
        }
        isVoiceRecording = !isVoiceRecording
        MultimodalClientSDK.sdk?.stopSpeaking() // Stop the bot's speech when the user starts speaking
    }
}

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    isVoiceRecording: Boolean,
    onRecordClick: () -> Unit,
    messages: List<UiChatMessage>,
    recognizedText: String
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            reverseLayout = true
        ) {
            items(messages) { messageItem ->
                MessageBubble(message = messageItem)
            }
        }

        ChatInputField(
            onSendClick = { text ->
                if (text.isNotBlank()) {
                    scope.launch {
                        Log.e("onclick text", "" + text)
                        MultimodalClientSDK.sdk?.sendMessage(text)
                    }
                }
                MultimodalClientSDK.sdk?.stopSpeaking() // Stop the bot's speech when the user sends a new message
            },
            onRecordClick = onRecordClick,
            recognizedText = recognizedText,
            isVoiceRecording = isVoiceRecording
        )
    }
}

@Composable
fun ChatInputField(
    modifier: Modifier = Modifier,
    onSendClick: (String) -> Unit,
    onRecordClick: () -> Unit,
    recognizedText: String,
    isVoiceRecording: Boolean
) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(recognizedText) {
        textState = TextFieldValue(recognizedText)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp, bottom = 8.dp, top = 8.dp)
                    .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp)),
                textStyle = MaterialTheme.typography.bodyMedium,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 8.dp, top = 8.dp),
                    ) {
                        if (textState.text.isEmpty()) {
                            Text(
                                text = "Type a message...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(
                onClick = {
                    onSendClick(textState.text)
                    textState = TextFieldValue("")
                },
                modifier = Modifier
                    .padding(0.dp)
                    .size(36.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
            if (MultimodalClientSDK.isSpeechEnabled()) {
                IconButton(
                    onClick = {
                        onRecordClick()
                    },
                    modifier = Modifier
                        .padding(0.dp)
                        .size(36.dp)
                ) {
                    if (isVoiceRecording) {
                        Image(
                            painter = painterResource(id = R.drawable.mic_24px),
                            contentDescription = "Stop record",
                            colorFilter = ColorFilter.tint(Color.Red)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.mic_24px),
                            contentDescription = "Start Record",
                            colorFilter = ColorFilter.tint(Color.Black)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: UiChatMessage) {
    val backgroundColor = if (message.isUser) Color(0xFFDCF8C6) else Color(0xFFE5E5E5)
    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .wrapContentWidth(alignment)
    ) {
        if (message.isAgentTyping) {
            TypingIndicator()
        } else if (message.customView != null) {
            AndroidView(
                factory = {
                    message.customView
                },
                modifier = Modifier
                    .background(backgroundColor, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
        } else {
            Text(
                text = message.message,
                modifier = Modifier
                    .background(backgroundColor, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    val animationDuration = 300

    val animationOffsets = remember {
        mutableStateListOf(0f, 0f, 0f)
    }

    LaunchedEffect(Unit) {
        while (true) {
            animationOffsets[0] = if (animationOffsets[0] == 0f) 8f else 0f
            delay(animationDuration.toLong())

            animationOffsets[1] = if (animationOffsets[1] == 0f) 8f else 0f
            delay((animationDuration * 0.15).toLong())

            animationOffsets[2] = if (animationOffsets[2] == 0f) 8f else 0f
            delay((animationDuration * 0.3).toLong())
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(animationOffsets[0])
        Dot(animationOffsets[1])
        Dot(animationOffsets[2])
    }
}

@Composable
fun Dot(offset: Float) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = offset.dp)
            .clip(CircleShape)
            .background(Color(0xFFE5E5E5))
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SampleApplicationTheme {
        Greeting("Android")
    }
}

data class UiChatMessage(
    val id: String,
    val message: String,
    val isUser: Boolean,
    val isAgentTyping: Boolean,
    val customView: View? = null
)

@Composable
fun MainScreen(
    isVoiceRecording: Boolean,
    onRecordClick: () -> Unit
) {
    val showAgent = remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(listOf<UiChatMessage>()) }
    var recognizedText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val messageResponse by MultimodalClientSDK.liveData.collectAsState()
    MultimodalClientSDK.sdk?.registerForContinuousListening(
        onRecognizing = { recognizedText = it },
        onRecognized = {
            Log.e("onRecognized", "" + recognizedText)
            recognizedText = it

            if (recognizedText.isNotBlank()) {
                val text = recognizedText
                recognizedText = ""
                scope.launch {
                    MultimodalClientSDK.sdk?.sendMessage(text)
                }
            }
        }
    )

    LaunchedEffect(messageResponse) {
        when (messageResponse) {
            MessageResponse.Initial -> { /* do nothing */
            }

            is MessageResponse.Success -> {
                val messageData = (messageResponse as MessageResponse.Success<ChatMessage>).value
                messages = if (messageData.role == "bot") {
                    if (messageData.customView != null) {
                        listOf(
                            UiChatMessage(
                                id = "bot-${System.currentTimeMillis()}",
                                message = messageData.text,
                                isUser = false,
                                isAgentTyping = false,
                                messageData.customView!!
                            )
                        ) + messages.filterNot { it.isAgentTyping }
                    } else {
                        listOf(
                            UiChatMessage(
                                id = "bot-${System.currentTimeMillis()}",
                                message = messageData.text,
                                isUser = false,
                                isAgentTyping = false
                            )
                        ) + messages.filterNot { it.isAgentTyping }
                    }

                } else {
                    listOf(
                        UiChatMessage(
                            id = "user-${System.currentTimeMillis()}",
                            message = messageData.text,
                            isUser = true,
                            isAgentTyping = false
                        )
                    ) + messages.filterNot { it.isAgentTyping }
                }
            }

            is MessageResponse.Typing -> {
                if (messages.none { it.isAgentTyping }) {
                    messages = listOf(
                        UiChatMessage(
                            id = "bot-${System.currentTimeMillis()}",
                            message = "Typing...",
                            isUser = false,
                            isAgentTyping = true
                        )
                    ) + messages
                }
            }

            is MessageResponse.Failure -> {
                val messageData = (messageResponse as MessageResponse.Failure<ChatMessage>).value
                messages = listOf(
                    UiChatMessage(
                        id = "bot-${System.currentTimeMillis()}",
                        message = messageData.text,
                        isUser = false,
                        isAgentTyping = false
                    )
                ) + messages.filterNot { it.isAgentTyping }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        VanillaApp()
        if (showAgent.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showAgent.value = false }
            ) {
                ChatScreen(
                    modifier = Modifier
                        .fillMaxWidth(0.95f) // Adjust the width to 95% of the screen width
                        .fillMaxHeight(0.70f) // Adjust the height to 70% of the screen height
                        .align(Alignment.BottomEnd) // Align to the bottom end (right)
                        .padding(16.dp) // Add padding around the screen
                        .background(
                            Color.White,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .border(
                            1.dp,
                            Color.Gray,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            )
                        ), // Add a border for better visibility
                    isVoiceRecording = isVoiceRecording,
                    onRecordClick = {
                        onRecordClick()
                        showAgent.value = false
                    },
                    messages = messages,
                    recognizedText = recognizedText
                )
            }
        } else {
            PillUI(
                isVoiceRecording = isVoiceRecording,
                onKeyboardClick = { showAgent.value = true },
                onMicClick = {
                    onRecordClick()
                    showAgent.value = false
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 35.dp)
            )
        }
    }
}

@Composable
fun PillUI(
    isVoiceRecording: Boolean,
    onKeyboardClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val micColor by animateColorAsState(
        targetValue = if (isVoiceRecording) Color.Red else Color(0xFF01678C),
        animationSpec = tween(durationMillis = 300)
    )

    val micSize by animateDpAsState(
        targetValue = if (isVoiceRecording) 54.dp else 54.dp,
        animationSpec = tween(durationMillis = 300)
    )

    val outlineWidth by animateDpAsState(
        targetValue = if (isVoiceRecording) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 1000)
    )

    Row(
        modifier = modifier
            .width(if (MultimodalClientSDK.isSpeechEnabled()) 165.dp else 90.dp)
            .height(60.dp)
            .background(Color(0xDCFFFFFF), shape = RoundedCornerShape(26.dp))
            .border(outlineWidth, Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(26.dp))
            .padding(horizontal = 10.dp)
            .boxShadow(isVoiceRecording)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onKeyboardClick) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_keyboard_24),
                contentDescription = "Keyboard",
                tint = Color(0xFF242424),
                modifier = Modifier.size(36.dp)
            )
        }

        if (MultimodalClientSDK.isSpeechEnabled()) {
            IconButton(onClick = onMicClick) {
                Image(
                    painter = painterResource(id = R.drawable.mic_24px),
                    contentDescription = if (isVoiceRecording) "Stop Recording" else "Start Recording",
                    colorFilter = ColorFilter.tint(micColor),
                    modifier = Modifier
                        .size(micSize)
                        .background(Color(0xFF95B8C3), shape = CircleShape)
                        .border(1.dp, Color.White, shape = CircleShape)
                        .innerShadow(isVoiceRecording)
                )
            }
        }
    }
}

@Composable
fun Modifier.boxShadow(isVoiceRecording: Boolean): Modifier {
    return if (isVoiceRecording) {
        this.shadow(20.dp, CircleShape, clip = false)
    } else {
        this
    }
}

@Composable
fun Modifier.innerShadow(isVoiceRecording: Boolean): Modifier {
    return if (isVoiceRecording) {
        this.graphicsLayer {
            shadowElevation = 20.dp.toPx()
            shape = CircleShape
            clip = true
        }
    } else {
        this
    }
}

@Composable
fun SetupScreen(
    token: String,
    onTokenChange: (String) -> Unit,
    enableSpeech: Boolean,
    onEnableSpeechChange: (Boolean) -> Unit,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter Directine Token", style = MaterialTheme.typography.titleMedium)
        BasicTextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (token.isEmpty()) {
                        Text(
                            text = "Enter your DirectLine token... else type NA",
                            color = Color.Gray
                        )
                    }
                    innerTextField()
                }
            }
        )
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.padding(vertical = 8.dp)
//        ) {
//            androidx.compose.material3.Switch(
//                checked = enableSpeech,
//                onCheckedChange = onEnableSpeechChange
//            )
//            Text("Enable Speech Service", modifier = Modifier.padding(start = 8.dp))
//        }
        androidx.compose.material3.Button(
            onClick = { if (token.isNotBlank()) onProceed() },
            enabled = token.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Proceed")
        }
    }
}