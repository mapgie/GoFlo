package com.mapgie.goflo.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    viewModel: PinSetupViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isDone) {
        if (state.isDone) onDone()
    }

    val title = when (state.step) {
        PinSetupStep.VERIFY_CURRENT -> "Enter current PIN"
        PinSetupStep.ENTER_NEW -> "Choose a new PIN"
        PinSetupStep.CONFIRM_NEW -> "Confirm your PIN"
    }
    val subtitle = when (state.step) {
        PinSetupStep.VERIFY_CURRENT -> "Verify your identity before changing your PIN"
        PinSetupStep.ENTER_NEW -> "Enter 4 digits"
        PinSetupStep.CONFIRM_NEW -> "Enter the same PIN again"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.step == PinSetupStep.VERIFY_CURRENT) "Change PIN" else "Set PIN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(40.dp))

            SetupPinDots(filledCount = state.enteredDigits.length, isError = state.isError)

            if (state.isError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }

            Spacer(Modifier.height(40.dp))

            // Reuse the same number pad layout from LockScreen
            SetupNumberPad(onDigit = viewModel::onDigit, onDelete = viewModel::onDelete)
        }
    }
}

@Composable
private fun SetupPinDots(filledCount: Int, isError: Boolean) {
    val dotColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.semantics {
            contentDescription = "$filledCount of 4 digits entered"
            liveRegion = LiveRegionMode.Polite
        }
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < filledCount) dotColor
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun SetupNumberPad(onDigit: (Int) -> Unit, onDelete: () -> Unit) {
    val keys = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    TextButton(onClick = { onDigit(digit) }, modifier = Modifier.size(72.dp)) {
                        Text(digit.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(Modifier.size(72.dp))
            TextButton(onClick = { onDigit(0) }, modifier = Modifier.size(72.dp)) {
                Text("0", style = MaterialTheme.typography.headlineMedium)
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(72.dp)
                    .semantics { contentDescription = "Delete" }
            ) {
                Text("⌫", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
