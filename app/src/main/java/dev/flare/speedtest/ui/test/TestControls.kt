package dev.flare.speedtest.ui.test

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestSize

/** Start (split button with test-size menu), stop while running, restart + share when done. */
@Composable
fun TestControls(
    state: TestUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onShare: () -> Unit,
    onTestSizeSelected: (TestSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when {
            state.isRunning -> Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(ButtonDefaults.MediumIconSize))
                Text(stringResource(R.string.test_stop), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMediumEmphasized)
            }

            state.phase == TestPhase.DONE -> ButtonGroup(
                overflowIndicator = { menu -> ButtonGroupDefaults.OverflowIndicator(menu) },
            ) {
                customItem(
                    buttonGroupContent = {
                        val source = remember { MutableInteractionSource() }
                        Button(
                            onClick = onStart,
                            interactionSource = source,
                            modifier = Modifier.animateWidth(source),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Text(stringResource(R.string.test_again), modifier = Modifier.padding(start = 8.dp))
                        }
                    },
                    menuContent = { menu ->
                        DropdownMenuItem(text = { Text(stringResource(R.string.test_again)) }, onClick = { menu.dismiss(); onStart() })
                    },
                )
                customItem(
                    buttonGroupContent = {
                        val source = remember { MutableInteractionSource() }
                        FilledTonalButton(
                            onClick = onShare,
                            interactionSource = source,
                            modifier = Modifier.animateWidth(source),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Text(stringResource(R.string.test_share), modifier = Modifier.padding(start = 8.dp))
                        }
                    },
                    menuContent = { menu ->
                        DropdownMenuItem(text = { Text(stringResource(R.string.test_share)) }, onClick = { menu.dismiss(); onShare() })
                    },
                )
            }

            else -> StartSplitButton(state.testSize, onStart, onTestSizeSelected)
        }
    }
}

@Composable
private fun StartSplitButton(testSize: TestSize, onStart: () -> Unit, onTestSizeSelected: (TestSize) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val sizeMenuDescription = stringResource(R.string.cd_test_size_menu)
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(onClick = onStart) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize))
                Text(
                    stringResource(R.string.test_start_with_size, stringResource(testSize.labelRes())),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        },
        trailingButton = {
            Box {
                SplitButtonDefaults.TrailingButton(
                    checked = menuOpen,
                    onCheckedChange = { menuOpen = it },
                    modifier = Modifier.semantics { contentDescription = sizeMenuDescription },
                ) {
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    TestSize.entries.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(stringResource(size.labelRes())) },
                            leadingIcon = {
                                if (size == testSize) Icon(Icons.Rounded.Check, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onTestSizeSelected(size)
                            },
                        )
                    }
                }
            }
        },
    )
}

fun TestSize.labelRes(): Int = when (this) {
    TestSize.SMALL -> R.string.size_small
    TestSize.NORMAL -> R.string.size_normal
    TestSize.LARGE -> R.string.size_large
}
