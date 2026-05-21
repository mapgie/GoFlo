package com.mapgie.goflo.ui.screens.disclaimer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DisclaimerScreen(onAcknowledge: () -> Unit) {
    // Back gesture does nothing — the disclaimer must be acknowledged.
    BackHandler {}

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Before You Continue",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            DisclaimerSection(
                heading = "Not a Medical Device",
                body = "GoFlo is a personal cycle-tracking tool only. It is not a medical device and " +
                        "is not intended to diagnose, treat, cure, or prevent any medical condition.\n\n" +
                        "Cycle predictions are estimates based on your logged history. They can be " +
                        "inaccurate, especially with irregular cycles, hormonal changes, or limited data."
            )

            DisclaimerSection(
                heading = "Not a Contraceptive",
                body = "GoFlo must not be used as a method of contraception or to plan or prevent " +
                        "pregnancy. No period-tracking app is a reliable substitute for medical " +
                        "contraceptive advice."
            )

            DisclaimerSection(
                heading = "Consult a Healthcare Professional",
                body = "Always seek the advice of a qualified healthcare provider for any questions " +
                        "about your reproductive health, menstrual cycle, or symptoms. Never disregard " +
                        "professional medical advice because of something you read or tracked in this app."
            )

            DisclaimerSection(
                heading = "Your Privacy",
                body = "All data you enter in GoFlo — period dates, symptoms, flow levels, and notes — " +
                        "is stored exclusively on your device. No data is ever transmitted, shared, " +
                        "or accessible to anyone else, including the developer.\n\n" +
                        "There are no accounts, no cloud sync, and no analytics of any kind."
            )

            HorizontalDivider()

            Text(
                text = "By tapping \"I Understand\" you confirm that you have read and understood " +
                        "this notice and agree to use GoFlo as a personal tracking aid only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onAcknowledge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I Understand")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DisclaimerSection(heading: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
