package com.example.c001apk.compose.ui.component.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.component.rememberHapticClick
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class MenuAction(val title: String, val icon: ImageVector? = null, val onClick: () -> Unit)

@Composable
fun MiuixCascadingMenuButton(
    entry: DropdownEntry,
    modifier: Modifier = Modifier,
    contentDescription: String = "更多选项",
) {
    var expanded by remember { mutableStateOf(false) }
    GlobalPopupBlur(expanded)
    ConfiguredMenuHaptics {
        OverlayIconCascadingDropdownMenu(
            entry = entry, modifier = modifier.contentPress(pressedScale = .92f),
            minWidth = 48.dp, minHeight = 48.dp, onExpandedChange = { expanded = it },
        ) {
            Icon(MiuixIcons.Medium.More, contentDescription,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        }
    }
}

@Composable
fun MiuixActionMenuButton(
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
    contentDescription: String = "更多选项",
) {
    var expanded by remember { mutableStateOf(false) }
    GlobalPopupBlur(expanded)
    ConfiguredMenuHaptics {
        OverlayIconDropdownMenu(entry = actionEntry(actions), modifier = modifier.contentPress(pressedScale = .92f),
            minWidth = 48.dp, minHeight = 48.dp, onExpandedChange = { expanded = it }) {
            Icon(MiuixIcons.Medium.More, contentDescription,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        }
    }
}

@Composable
fun MiuixActionMenu(expanded: Boolean, onDismiss: () -> Unit, actions: List<MenuAction>) {
    GlobalPopupBlur(expanded)
    ConfiguredMenuHaptics {
        OverlayDropdownPopup(entry = actionEntry(actions), show = expanded, onDismiss = onDismiss,
            onDismissFinished = {}, maxHeight = null,
            dropdownColors = DropdownDefaults.dropdownColors(), renderInRootScaffold = true)
    }
}

@Composable
private fun actionEntry(actions: List<MenuAction>): DropdownEntry {
    val foreground = MiuixTheme.colorScheme.onSurface
    return DropdownEntry(actions.map { action ->
        DropdownItem(text = action.title, onClick = action.onClick,
            icon = action.icon?.let { vector ->
                { modifier ->
                    Icon(vector, null, modifier.testTag("action-menu-icon-${action.title}"), tint = foreground)
                }
            })
    })
}

@Composable
private fun ConfiguredMenuHaptics(content: @Composable () -> Unit) {
    val click = rememberHapticClick {}
    val haptics = remember(click) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = click()
        }
    }
    CompositionLocalProvider(LocalHapticFeedback provides haptics, content = content)
}
