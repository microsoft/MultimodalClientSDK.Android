package com.example.sampleapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun VanillaApp() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.ai_img),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, start = 24.dp, end = 24.dp)
        ) {
            androidx.compose.material3.Text(
                text = "Welcome to the Sample app",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = androidx.compose.ui.graphics.Color.White
            )
            androidx.compose.material3.Text(
                text = "This app uses the Multimodal client SDK, enabling you to explore its diverse features through both text and voice interactions.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(top = 12.dp)
            )
            // Add Spacer or other content as needed
        }
    }
}