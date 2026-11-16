package com.fildy.ui.home.farm

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fildy.R
import com.fildy.data.model.CattleCategory
import com.fildy.ui.components.DataCard
import com.fildy.ui.components.DataCardHeader
import com.fildy.ui.components.EnterFromBelow
import com.fildy.ui.components.formatted
import com.fildy.ui.components.rememberAppLocale
import com.fildy.ui.theme.Divider
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.MentaClaro
import com.fildy.ui.theme.Musgo
import com.fildy.ui.theme.Pino
import com.fildy.ui.theme.Tinta
import com.fildy.ui.theme.forestBackground
import java.util.Locale

/**
 * "Mi campo": paddocks, total head and head per category, for the whole farm. It slides in once
 * the farm is loaded, after [enterDelayMillis].
 */
@Composable
fun FarmCard(
    modifier: Modifier = Modifier,
    enterDelayMillis: Int = 0,
    viewModel: FarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded = uiState as? FarmUiState.Success
    EnterFromBelow(visible = loaded != null, modifier = modifier, delayMillis = enterDelayMillis) {
        loaded?.let { FarmCard(it) }
    }
}

/** Categories per row: 3 on top, 2 below, so every label fits at its full size. */
private const val FIRST_ROW_SIZE = 3

@Composable
fun FarmCard(state: FarmUiState.Success, modifier: Modifier = Modifier) {
    val locale = rememberAppLocale()
    DataCard(modifier) {
        DataCardHeader(
            icon = R.drawable.ic_sprout,
            iconTint = Pino,
            iconBackground = MentaClaro,
            title = stringResource(R.string.farm_card_title),
            subtitle = pluralStringResource(R.plurals.farm_card_paddocks, state.paddockCount, state.paddockCount),
            value = state.totalHeadCount.formatted(locale),
            unit = stringResource(R.string.farm_card_head_unit),
        )
        Spacer(Modifier.height(14.dp))
        CountsRow(state.headCounts.take(FIRST_ROW_SIZE), locale)
        Spacer(
            Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Divider),
        )
        CountsRow(state.headCounts.drop(FIRST_ROW_SIZE), locale)
    }
}

/** Equal-width category columns separated by thin vertical dividers. */
@Composable
private fun CountsRow(counts: List<CategoryCount>, locale: Locale) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        counts.forEachIndexed { index, count ->
            if (index > 0) VerticalDivider()
            CategoryColumn(count, locale)
        }
    }
}

@Composable
private fun RowScope.CategoryColumn(count: CategoryCount, locale: Locale) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f),
    ) {
        Text(
            text = count.headCount.formatted(locale),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = (-0.3).sp),
            fontWeight = FontWeight.ExtraBold,
            color = Tinta,
        )
        Text(
            text = stringResource(count.category.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = Musgo,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Spacer(
        Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Divider),
    )
}

@get:StringRes
val CattleCategory.labelRes: Int
    get() = when (this) {
        CattleCategory.COW -> R.string.cattle_cow
        CattleCategory.HEIFER -> R.string.cattle_heifer
        CattleCategory.CALF -> R.string.cattle_calf
        CattleCategory.STEER -> R.string.cattle_steer
        CattleCategory.BULL -> R.string.cattle_bull
    }

@Preview(widthDp = 360)
@Composable
private fun FarmCardPreview() {
    FildyTheme {
        FarmCard(
            state = FarmUiState.Success(
                paddockCount = 8,
                totalHeadCount = 2247,
                headCounts = listOf(
                    CategoryCount(CattleCategory.COW, 450),
                    CategoryCount(CattleCategory.HEIFER, 448),
                    CategoryCount(CattleCategory.CALF, 452),
                    CategoryCount(CattleCategory.STEER, 447),
                    CategoryCount(CattleCategory.BULL, 450),
                ),
            ),
            modifier = Modifier
                .forestBackground()
                .padding(18.dp),
        )
    }
}
