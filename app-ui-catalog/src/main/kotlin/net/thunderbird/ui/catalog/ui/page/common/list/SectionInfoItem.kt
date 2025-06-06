package net.thunderbird.ui.catalog.ui.page.common.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme

fun LazyGridScope.sectionInfoItem(
    text: String,
) {
    fullSpanItem {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MainTheme.spacings.double,
                    end = MainTheme.spacings.double,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextBodySmall(
                text = text,
            )
        }
    }
}
