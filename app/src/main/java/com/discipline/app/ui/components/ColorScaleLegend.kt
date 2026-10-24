package com.discipline.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.discipline.app.domain.DisciplineScoringEngine
import com.discipline.app.ui.theme.ScoreColor0
import com.discipline.app.ui.theme.ScoreColor100
import com.discipline.app.ui.theme.ScoreColor1to19
import com.discipline.app.ui.theme.ScoreColor20to39
import com.discipline.app.ui.theme.ScoreColor40to59
import com.discipline.app.ui.theme.ScoreColor60to79
import com.discipline.app.ui.theme.ScoreColor80to99
import com.discipline.app.ui.theme.ScoreColorNone

data class LegendItem(val label: String, val color: Color, val hex: String)

val scoreLegendItems = listOf(
    LegendItem("100%", ScoreColor100, "#064E3B"),
    LegendItem("80–99%", ScoreColor80to99, "#10B981"),
    LegendItem("60–79%", ScoreColor60to79, "#84CC16"),
    LegendItem("40–59%", ScoreColor40to59, "#F59E0B"),
    LegendItem("20–39%", ScoreColor20to39, "#F97316"),
    LegendItem("1–19%", ScoreColor1to19, "#EF4444"),
    LegendItem("0%", ScoreColor0, "#7F1D1D"),
    LegendItem("No habits", ScoreColorNone, "#6B7280")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorScaleLegend(
    modifier: Modifier = Modifier
) {
    DisciplineCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Discipline Color Scale",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (item in scoreLegendItems) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(item.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
