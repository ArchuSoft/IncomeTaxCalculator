package com.example.incometaxcalculator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// import androidx.compose.material3.rememberRipple // Remove this import
import androidx.compose.material3.ripple // Add this import
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LargeActionButton(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = title }
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true), // Changed here
                onClick = onClick
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
