package com.mapgie.goflo.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.theme.ComfortaaFamily
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    onUnlocked: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Back gesture does nothing on the lock screen.
    BackHandler {}

    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) onUnlocked()
    }

    LaunchedEffect(state.biometricEnabled) {
        if (state.biometricEnabled) {
            showBiometricPrompt(
                activity = context as FragmentActivity,
                onSuccess = { viewModel.onBiometricSuccess() }
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("GoFlo", style = MaterialTheme.typography.headlineLarge, fontFamily = ComfortaaFamily)
            Spacer(Modifier.height(8.dp))
            Text("Enter your PIN to continue", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(40.dp))

            PinDots(filledCount = state.enteredDigits.length, isError = state.isError)

            if (state.isError) {
                Spacer(Modifier.height(8.dp))
                Text("Incorrect PIN", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(40.dp))

            NumberPad(
                onDigit = viewModel::onDigit,
                onDelete = viewModel::onDelete,
                showBiometric = state.biometricEnabled,
                onBiometric = {
                    showBiometricPrompt(
                        activity = context as FragmentActivity,
                        onSuccess = { viewModel.onBiometricSuccess() }
                    )
                }
            )
        }
    }
}

@Composable
private fun PinDots(filledCount: Int, isError: Boolean) {
    val dotColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
private fun NumberPad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    showBiometric: Boolean,
    onBiometric: () -> Unit
) {
    val keys = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9)
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    PadKey(label = digit.toString(), onClick = { onDigit(digit) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showBiometric) {
                FilledTonalIconButton(onClick = onBiometric, modifier = Modifier.size(72.dp)) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometric unlock",
                        modifier = Modifier.size(32.dp))
                }
            } else {
                Spacer(Modifier.size(72.dp))
            }
            PadKey(label = "0", onClick = { onDigit(0) })
            TextButton(onClick = onDelete, modifier = Modifier.size(72.dp)) {
                Text("⌫", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun PadKey(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp)
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
    }
}

private fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val canAuthenticate = BiometricManager.from(activity)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) return

    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock GoFlo")
        .setSubtitle("Use your biometric credential")
        .setNegativeButtonText("Use PIN")
        .build()
    prompt.authenticate(info)
}
