package com.bugdigger.piggybank.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bugdigger.piggybank.ui.theme.PiggyBankColors

/**
 * An editable text cell for the register spreadsheet
 */
@Composable
fun EditableCell(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onEditStart: () -> Unit = {},
    onEditEnd: () -> Unit = {},
    onTab: () -> Unit = {},
    onShiftTab: () -> Unit = {},
    onEnter: () -> Unit = {},
    onEscape: () -> Unit = {},
    textAlign: TextAlign = TextAlign.Start,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    textColor: Color = Color.Unspecified,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember(value) { 
        mutableStateOf(TextFieldValue(value, TextRange(value.length))) 
    }
    
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            // Select all text when entering edit mode
            textFieldValue = TextFieldValue(value, TextRange(0, value.length))
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onEditStart)
            .then(
                if (isEditing) {
                    Modifier.border(1.dp, PiggyBankColors.CellBorderFocused)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp),
        contentAlignment = when (textAlign) {
            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }
    ) {
        if (isEditing) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onValueChange(newValue.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && isEditing) {
                            onEditEnd()
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when {
                                event.key == Key.Tab && event.isShiftPressed -> {
                                    onShiftTab()
                                    true
                                }
                                event.key == Key.Tab -> {
                                    onTab()
                                    true
                                }
                                event.key == Key.Enter -> {
                                    onEnter()
                                    true
                                }
                                event.key == Key.Escape -> {
                                    onEscape()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                textStyle = textStyle.copy(
                    color = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = textAlign
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onTab() }
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = when (textAlign) {
                            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
                            TextAlign.Center -> Alignment.Center
                            else -> Alignment.CenterStart
                        }
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            Text(
                text = value.ifEmpty { placeholder },
                style = textStyle,
                color = if (value.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                } else if (textColor != Color.Unspecified) {
                    textColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = textAlign,
                maxLines = 1
            )
        }
    }
}

/**
 * Account selector dropdown for the Transfer field
 */
@Composable
fun AccountSelectorCell(
    value: String,
    onValueChange: (String, String) -> Unit, // (accountId, accountFullName)
    accounts: List<Pair<String, String>>, // List of (id, fullName)
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onEditStart: () -> Unit = {},
    onEditEnd: () -> Unit = {},
    onTab: () -> Unit = {},
    onShiftTab: () -> Unit = {},
    onEnter: () -> Unit = {},
    onEscape: () -> Unit = {},
    placeholder: String = ""
) {
    var searchText by remember(value) { mutableStateOf(value) }
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    val filteredAccounts = remember(searchText, accounts) {
        if (searchText.isBlank()) {
            accounts
        } else {
            accounts.filter { (_, name) ->
                name.contains(searchText, ignoreCase = true)
            }
        }
    }
    
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            expanded = true
        } else {
            expanded = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onEditStart)
    ) {
        if (isEditing) {
            Column {
                BasicTextField(
                    value = searchText,
                    onValueChange = { 
                        searchText = it
                        expanded = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .border(1.dp, PiggyBankColors.CellBorderFocused)
                        .padding(horizontal = 4.dp)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when {
                                    event.key == Key.Tab && event.isShiftPressed -> {
                                        onShiftTab()
                                        true
                                    }
                                    event.key == Key.Tab -> {
                                        onTab()
                                        true
                                    }
                                    event.key == Key.Enter -> {
                                        // Select first match if available
                                        filteredAccounts.firstOrNull()?.let { (id, name) ->
                                            onValueChange(id, name)
                                            searchText = name
                                        }
                                        onEnter()
                                        true
                                    }
                                    event.key == Key.Escape -> {
                                        onEscape()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
                
                DropdownMenu(
                    expanded = expanded && filteredAccounts.isNotEmpty(),
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.widthIn(min = 200.dp, max = 400.dp)
                ) {
                    filteredAccounts.take(10).forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onValueChange(id, name)
                                searchText = name
                                expanded = false
                                onTab()
                            }
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifEmpty { placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Amount cell with special handling for numeric input
 */
@Composable
fun AmountEditableCell(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onEditStart: () -> Unit = {},
    onEditEnd: () -> Unit = {},
    onTab: () -> Unit = {},
    onShiftTab: () -> Unit = {},
    onEnter: () -> Unit = {},
    onEscape: () -> Unit = {},
    textColor: Color = Color.Unspecified
) {
    EditableCell(
        value = value,
        onValueChange = { newValue ->
            // Only allow numeric input with decimal point
            val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
            onValueChange(filtered.replace(',', '.'))
        },
        modifier = modifier,
        isEditing = isEditing,
        onEditStart = onEditStart,
        onEditEnd = onEditEnd,
        onTab = onTab,
        onShiftTab = onShiftTab,
        onEnter = onEnter,
        onEscape = onEscape,
        textAlign = TextAlign.End,
        textStyle = MaterialTheme.typography.bodyMedium,
        textColor = textColor,
        keyboardType = KeyboardType.Decimal
    )
}
