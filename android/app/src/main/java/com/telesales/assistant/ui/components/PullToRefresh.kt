package com.telesales.assistant.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

data class PullToRefreshGesture(val modifier: Modifier, val isPulling: Boolean)

@Composable
fun rememberPullToRefreshGesture(
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
): PullToRefreshGesture {
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val connection = remember(listState, isRefreshing, onRefresh) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    pullDistance += available.y
                    return Offset(0f, available.y)
                }
                if (available.y < 0) pullDistance = 0f
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance >= 90f && !isRefreshing) onRefresh()
                pullDistance = 0f
                return Velocity.Zero
            }
        }
    }
    return PullToRefreshGesture(Modifier.nestedScroll(connection), pullDistance > 0f)
}
