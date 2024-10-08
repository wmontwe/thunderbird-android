package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
fun ContributionFooterPreview() {
    PreviewWithTheme {
        ContributionFooter(
            onClick = {},
            isPurchaseEnabled = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterDisabledPreview() {
    PreviewWithTheme {
        ContributionFooter(
            onClick = {},
            isPurchaseEnabled = false,
        )
    }
}
