package com.aistudio.liquidglass.lnchr.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.liquidglass.lnchr.ui.glass.LiquidGlassSurface
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MinimalClock(
    is24Hour: Boolean,
    showDate: Boolean,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L) // Update every second
        }
    }

    val timeFormat = remember(is24Hour) {
        if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault())
        else SimpleDateFormat("h:mm", Locale.getDefault())
    }

    val amPmFormat = remember(is24Hour) {
        if (!is24Hour) SimpleDateFormat("a", Locale.getDefault()) else null
    }

    val dateFormat = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    }

    val timeString = timeFormat.format(currentTime)
    val amPmString = amPmFormat?.format(currentTime)?.uppercase(Locale.getDefault()) ?: ""
    val dateString = dateFormat.format(currentTime)

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        cornerRadius = 28.dp,
        opacity = 0.65f,
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = timeString,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraLight,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    letterSpacing = (-1.5).sp
                )

                if (amPmString.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPmString,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }

            if (showDate) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateString,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
