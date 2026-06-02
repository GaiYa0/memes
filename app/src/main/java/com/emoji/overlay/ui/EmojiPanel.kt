package com.emoji.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Placeholder emoji data for the grid.
 * Will be replaced with real emoji data in a future step.
 */
private val placeholderEmojis = listOf(
    "😀", "😂", "🥹", "😍", "🤩", "😎", "🥳", "😏",
    "🤔", "😤", "😭", "😱", "🙄", "😴", "🤗", "😈",
    "👻", "💀", "🤖", "👽", "🎃", "🌈", "⭐", "🔥",
    "❤️", "💔", "💯", "✅", "❌", "⚡", "🎉", "🎵",
    "🐱", "🐶", "🦊", "🐼", "🐸", "🦋", "🌸", "🍕",
    "🚀", "✈️", "🏠", "📱", "💻", "🎮", "📷", "🎸",
)

/**
 * Main emoji panel composable with slide-in/out animation.
 *
 * @param visible whether the panel should be shown
 * @param modifier modifier for the panel
 */
@Composable
fun EmojiPanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onEmojiSelected: (String) -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 200)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 200)
        ),
        modifier = modifier
    ) {
        EmojiPanelContent(onEmojiSelected = onEmojiSelected)
    }
}

/**
 * The actual panel content - a scrollable grid of emojis.
 */
@Composable
private fun EmojiPanelContent(
    onEmojiSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCCCCCC))
            )
        }

        // Emoji grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            items(placeholderEmojis) { emoji ->
                EmojiItem(emoji = emoji, onClick = { onEmojiSelected(emoji) })
            }
        }

        // Bottom padding for navigation bar
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

/**
 * Individual emoji item in the grid.
 */
@Composable
private fun EmojiItem(
    emoji: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}
