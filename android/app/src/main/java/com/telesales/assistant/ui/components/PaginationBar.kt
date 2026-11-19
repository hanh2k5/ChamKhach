package com.telesales.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Modern Apple / Facebook styled smart pagination bar with direct page jumping.
 *
 * Features:
 * - Compact sleek rounded-rect buttons (32dp height, 8dp corner radius).
 * - Compact arrows `‹` and `›` instead of bulky text.
 * - Quick jump input field: type any page number and jump instantly.
 */
@Composable
fun PaginationBar(
    currentPage: Int,
    lastPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (lastPage <= 1) return

    val slots = buildPageSlots(currentPage, lastPage)
    val focusManager = LocalFocusManager.current
    var inputPageText by remember(currentPage) { mutableStateOf(currentPage.toString()) }

    fun submitJump() {
        val target = inputPageText.toIntOrNull() ?: currentPage
        val clamped = target.coerceIn(1, lastPage)
        inputPageText = clamped.toString()
        focusManager.clearFocus()
        if (clamped != currentPage) {
            onPageSelected(clamped)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Row 1: Apple/FB styled page chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Previous button ‹
            Surface(
                onClick = { onPageSelected(currentPage - 1) },
                enabled = currentPage > 1,
                shape = RoundedCornerShape(8.dp),
                color = if (currentPage > 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier
                    .height(32.dp)
                    .width(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentPage > 1) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
            }

            // Page numbers
            for (slot in slots) {
                if (slot == ELLIPSIS) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val isSelected = slot == currentPage
                    Surface(
                        onClick = { onPageSelected(slot) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(min = 32.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = slot.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Next button ›
            Surface(
                onClick = { onPageSelected(currentPage + 1) },
                enabled = currentPage < lastPage,
                shape = RoundedCornerShape(8.dp),
                color = if (currentPage < lastPage) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier
                    .height(32.dp)
                    .width(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentPage < lastPage) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
            }
        }

        // Row 2: Direct Jump Input (Đến trang: [ 95 ] / 100 [ Đi ])
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Đến trang",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Number Input Box
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = inputPageText,
                    onValueChange = { newValue ->
                        if (newValue.all(Char::isDigit) && newValue.length <= 4) {
                            inputPageText = newValue
                        }
                    },
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { submitJump() }),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }

            Text(
                text = "/ $lastPage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Go Button
            Surface(
                onClick = { submitJump() },
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .height(30.dp)
                    .widthIn(min = 36.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Đi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

private const val ELLIPSIS = -1

/**
 * Returns an ordered list of page slots:
 *   positive int  → a page button
 *   ELLIPSIS (-1) → "…" spacer
 *
 * Always includes page 1 and lastPage.
 * Shows [currentPage-1, currentPage, currentPage+1] in the middle.
 */
internal fun buildPageSlots(currentPage: Int, lastPage: Int): List<Int> {
    if (lastPage <= 7) return (1..lastPage).toList()

    val result = mutableListOf<Int>()

    // Left anchor
    result += 1

    val leftNeighbour = currentPage - 1
    val rightNeighbour = currentPage + 1

    // Gap between page 1 and leftNeighbour?
    when {
        leftNeighbour > 3 -> result += ELLIPSIS          // 1 … [n-1 n n+1] …? last
        leftNeighbour == 3 -> result += 2                 // 1 2 [n-1 n n+1] …? last
        leftNeighbour == 2 -> result += 2                 // already consecutive with 1
        // leftNeighbour == 1 → currentPage == 2, nothing to add
    }

    // Middle band: leftNeighbour…rightNeighbour (skip 1 and lastPage, they're anchors)
    for (p in maxOf(2, leftNeighbour)..minOf(lastPage - 1, rightNeighbour)) {
        result += p
    }

    // Gap between rightNeighbour and lastPage?
    when {
        rightNeighbour < lastPage - 2 -> result += ELLIPSIS
        rightNeighbour == lastPage - 2 -> result += lastPage - 1
        rightNeighbour == lastPage - 1 -> result += lastPage - 1 // consecutive
        // rightNeighbour >= lastPage - 1 → nothing needed
    }

    // Right anchor
    result += lastPage

    val distinctResult = mutableListOf<Int>()
    for (item in result) {
        if (item == ELLIPSIS || !distinctResult.contains(item)) {
            distinctResult += item
        }
    }
    return distinctResult
}
