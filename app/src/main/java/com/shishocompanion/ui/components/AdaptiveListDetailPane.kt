package com.shishocompanion.ui.components

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * A reusable List-Detail scaffold component that leverages Material 3 Adaptive Navigation
 * and supports single/dual-pane modes based on window size and user split-screen preference.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T : Any> AdaptiveListDetailPane(
    splitScreen: Boolean,
    modifier: Modifier = Modifier,
    listPane: @Composable (onItemClick: (T) -> Unit) -> Unit,
    detailPane: @Composable (selectedItem: T, isDualPane: Boolean, onNavigateBack: () -> Unit) -> Unit,
    placeholderPane: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<T>(
        scaffoldDirective = calculatePaneScaffoldDirective(
            currentWindowAdaptiveInfo()
        ).copy(maxHorizontalPartitions = if (splitScreen) 2 else 1)
    )

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                listPane { id ->
                    coroutineScope.launch {
                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                val selectedId = scaffoldNavigator.currentDestination?.contentKey
                if (selectedId != null) {
                    val isDualPane = scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
                            scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                    detailPane(selectedId, isDualPane) {
                        coroutineScope.launch {
                            scaffoldNavigator.navigateBack()
                        }
                    }
                } else {
                    placeholderPane()
                }
            }
        }
    )
}
