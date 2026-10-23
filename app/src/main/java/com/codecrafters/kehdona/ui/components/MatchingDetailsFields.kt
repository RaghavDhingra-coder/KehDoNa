package com.codecrafters.kehdona.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference
import com.codecrafters.kehdona.ui.theme.Ink
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.LineStrong
import com.codecrafters.kehdona.ui.theme.Plum800
import com.codecrafters.kehdona.ui.theme.Rose300

/**
 * "I am" and "Who would you like to match with?" pickers. Nothing is preselected for gender;
 * the preference only gets a suggested default once a gender is chosen.
 */
@Composable
fun MatchingDetailsFields(
    gender: Gender?,
    selfDescription: String,
    preference: MatchPreference?,
    onGenderChange: (Gender) -> Unit,
    onSelfDescriptionChange: (String) -> Unit,
    onPreferenceChange: (MatchPreference) -> Unit,
    modifier: Modifier = Modifier,
    genderError: String? = null,
    selfDescriptionError: String? = null,
    preferenceError: String? = null,
    enabled: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PickerLabel("I am", genderError)
        ChoiceChips(Gender.entries, gender, { it.label }, onGenderChange, enabled, genderError != null)
        if (gender == Gender.SelfDescribe) {
            KehDoNaTextField(
                value = selfDescription,
                onValueChange = onSelfDescriptionChange,
                label = "How do you describe your gender? (optional)",
                error = selfDescriptionError,
                supportingText = "Private to you. It's never shown to matches.",
                enabled = enabled,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        PickerLabel("Who would you like to match with?", preferenceError)
        ChoiceChips(MatchPreference.entries, preference, { it.label }, onPreferenceChange, enabled, preferenceError != null)
    }
}

@Composable
private fun PickerLabel(text: String, error: String?) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = InkMuted, modifier = Modifier.semantics { heading() })
    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean,
    isError: Boolean,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                enabled = enabled,
                label = { Text(label(option), style = MaterialTheme.typography.labelLarge) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    this.selected = isSelected
                    if (isError) error("Required")
                },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = InkMuted,
                    selectedContainerColor = Plum800,
                    selectedLabelColor = Ink,
                    selectedLeadingIconColor = Rose300,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = enabled,
                    selected = isSelected,
                    borderColor = if (isError) MaterialTheme.colorScheme.error else LineStrong,
                    selectedBorderColor = Rose300.copy(alpha = 0.5f),
                    disabledBorderColor = InkSubtle.copy(alpha = 0.3f),
                ),
            )
        }
    }
}
