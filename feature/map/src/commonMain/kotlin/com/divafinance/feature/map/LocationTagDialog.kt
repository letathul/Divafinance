package com.divafinance.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.DivaTextField

@Composable
fun LocationTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, latitude: Double, longitude: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var latError by remember { mutableStateOf(false) }
    var lngError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag Location") },
        text = {
            Column {
                DivaTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = "Location Name",
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DivaTextField(
                        value = latitude,
                        onValueChange = {
                            latitude = it
                            latError = false
                        },
                        label = "Latitude",
                        isError = latError,
                        modifier = Modifier.weight(1f),
                    )
                    DivaTextField(
                        value = longitude,
                        onValueChange = {
                            longitude = it
                            lngError = false
                        },
                        label = "Longitude",
                        isError = lngError,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (nameError || latError || lngError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Please fill in all fields with valid values",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    nameError = name.isBlank()
                    latError = lat == null || lat < -90 || lat > 90
                    lngError = lng == null || lng < -180 || lng > 180
                    if (!nameError && !latError && !lngError) {
                        onConfirm(name, lat!!, lng!!)
                    }
                },
            ) {
                Text("Tag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
