package com.telesales.assistant.ui.dashboard

import android.graphics.Paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.R
import com.telesales.assistant.data.repository.DashboardRepository
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.reminder.ReminderCoordinator
import com.telesales.assistant.ui.components.rememberPullToRefreshGesture
import java.time.Clock
import java.time.ZoneId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentWidth

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardRoute(
    repository: DashboardRepository,
    followUpRepository: FollowUpRepository,
    onSettings: () -> Unit,
    onOpenOverdue: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenPotential: () -> Unit,
    onAddPotential: () -> Unit,
    shouldRefresh: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
    clock: Clock = Clock.systemUTC(),
    businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    reminderCoordinator: ReminderCoordinator? = null,
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardFactory(repository, clock, businessZone, followUpRepository, reminderCoordinator))
    val state by vm.state.collectAsState()
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            vm.refresh()
            onRefreshConsumed()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val firstResume = remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume.value) firstResume.value = false else vm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DashboardScreen(state, vm, onSettings, onOpenOverdue, onOpenToday, onOpenPotential, onAddPotential)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DashboardScreen(
    state: DashboardUiState,
    vm: DashboardViewModel,
    onSettings: () -> Unit,
    onOpenOverdue: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenPotential: () -> Unit,
    onAddPotential: () -> Unit,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val pullRefresh = rememberPullToRefreshGesture(listState, state.isRefreshing) { vm.refresh() }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title), maxLines = 1, overflow = TextOverflow.Ellipsis) }, colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isRefreshing || pullRefresh.isPulling) LinearProgressIndicator(Modifier.fillMaxWidth())
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().then(pullRefresh.modifier).padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.dashboard != null) {
                    item {
                        val overdue = state.dashboard.overdueCount
                        val today = state.dashboard.todayCount
                        val potential = state.dashboard.potentialCount
                        DashboardChart(
                            overdue = overdue,
                            today = today,
                            potential = potential,
                            onOpenOverdue = onOpenOverdue,
                            onOpenToday = onOpenToday,
                            onOpenPotential = onOpenPotential,
                            onAddPotential = onAddPotential,
                        )
                    }
                    state.error?.let { error ->
                        item {
                            DashboardErrorMessage(
                                error = error,
                                onRetry = vm::retry,
                                onSettings = onSettings,
                            )
                        }
                    }
                } else if (state.isLoading) {
                    item { CircularProgressIndicator(Modifier.padding(12.dp)) }
                } else {
                    item {
                        DashboardErrorMessage(
                            error = state.error ?: DashboardError.Server,
                            onRetry = vm::retry,
                            onSettings = onSettings,
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun DashboardErrorMessage(
    error: DashboardError,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = errorText(error),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.customers_retry))
                }
                if (error == DashboardError.Unauthorized || error == DashboardError.SetupRequired) {
                    TextButton(onClick = onSettings) {
                        Text(stringResource(R.string.customers_open_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardChart(
    overdue: Int,
    today: Int,
    potential: Int,
    onOpenOverdue: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenPotential: () -> Unit,
    onAddPotential: () -> Unit,
) {
    val labels = listOf(
        stringResource(R.string.dashboard_overdue),
        stringResource(R.string.dashboard_today_short),
        stringResource(R.string.dashboard_potential_short),
    )
    val values = listOf(overdue, today, potential)
    val colors = listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val actions = listOf(onOpenOverdue, onOpenToday, onOpenPotential)
    val maxValue = maxOf(values.maxOrNull() ?: 0, 1)
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(7f, 7f), 0f) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(0.dp),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(246.dp)) {
                Canvas(Modifier.fillMaxSize().padding(start = 32.dp, end = 8.dp, top = 30.dp, bottom = 38.dp)) {
                    val baseline = size.height
                    val chartHeight = size.height - 8.dp.toPx()
                    for (step in 1..4) {
                        val y = baseline - chartHeight * step / 4f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.5f, pathEffect = dash)
                    }
                    drawLine(axisColor, Offset(0f, baseline), Offset(size.width, baseline), 2f, cap = StrokeCap.Round)
                    drawLine(axisColor, Offset(0f, baseline), Offset(0f, 0f), 2f, cap = StrokeCap.Round)
                    drawLine(axisColor, Offset(size.width, baseline), Offset(size.width - 12f, baseline - 6f), 2f, cap = StrokeCap.Round)
                    drawLine(axisColor, Offset(size.width, baseline), Offset(size.width - 12f, baseline + 6f), 2f, cap = StrokeCap.Round)
                    drawLine(axisColor, Offset(0f, 0f), Offset(-6f, 12f), 2f, cap = StrokeCap.Round)
                    drawLine(axisColor, Offset(0f, 0f), Offset(6f, 12f), 2f, cap = StrokeCap.Round)
                    val slotWidth = size.width / values.size
                    values.forEachIndexed { index, value ->
                        val barWidth = minOf(48.dp.toPx(), slotWidth * 0.46f)
                        val barHeight = maxOf(8.dp.toPx(), chartHeight * value / maxValue)
                        val left = slotWidth * index + (slotWidth - barWidth) / 2f
                        drawRect(
                            color = colors[index],
                            topLeft = Offset(left, baseline - barHeight),
                            size = Size(barWidth, barHeight),
                        )
                        labelPaint.apply {
                            color = colors[index].toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = 14.dp.toPx()
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            value.toString(),
                            left + barWidth / 2f,
                            baseline - barHeight - 6.dp.toPx(),
                            labelPaint,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxSize().padding(start = 32.dp, end = 8.dp, top = 30.dp, bottom = 38.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = androidx.compose.ui.Alignment.Bottom,
                ) {
                    actions.forEachIndexed { index, action ->
                        Box(
                            Modifier.weight(1f).fillMaxSize().clickable(onClick = action),
                            contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
                        ) {
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                labels.forEach { label ->
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(end = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = onAddPotential,
                                    // 1. Ép tất cả các lề bên trong về 0
                                    contentPadding = PaddingValues(0.dp),
                                    // 2. Phá bỏ kích thước tối thiểu mặc định để nút co lại hết cỡ
                                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.dashboard_add_potential),
                                        maxLines = 1,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            }
                        }
                    }
                    }
                }
            }
        }



@Composable
private fun errorText(error: DashboardError): String = when (error) {
    DashboardError.SetupRequired -> stringResource(R.string.dashboard_setup_required)
    DashboardError.Network -> stringResource(R.string.customers_network_error)
    DashboardError.Unauthorized -> stringResource(R.string.customers_unauthorized)
    DashboardError.Server -> stringResource(R.string.customers_server_error)
}

private class DashboardFactory(private val repository: DashboardRepository, private val clock: Clock, private val zone: ZoneId, private val followUpRepository: FollowUpRepository, private val reminderCoordinator: ReminderCoordinator?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
        return modelClass.cast(DashboardViewModel(repository, clock, zone, followUpRepository, reminderCoordinator))
    }
}
