package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.oauth.ui.preview.PreviewAccountOAuthViewModel
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.ViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeIncomingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeOutgoingServerValidationViewModel

@Composable
internal fun ServerValidationMainScreen(
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val dispatch = { event: Event -> viewModel.event(event) }

    Scaffold(
        topBar = {
            AppTitleTopHeader()
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = {},
                onBackClick = { dispatch(Event.OnBackClicked) },
                state = WizardNavigationBarState(
                    showNext = false,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        ServerValidationContent(
            onEvent = { dispatch(it) },
            state = state.value,
            isIncomingValidation = viewModel.isIncomingValidation,
            oAuthViewModel = viewModel.oAuthViewModel,
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun IncomingServerValidationScreenK9Preview() {
    K9Theme {
        ServerValidationMainScreen(
            viewModel = FakeIncomingServerValidationViewModel(
                oAuthViewModel = PreviewAccountOAuthViewModel(),
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun IncomingServerValidationScreenThunderbirdPreview() {
    ThunderbirdTheme {
        ServerValidationMainScreen(
            viewModel = FakeIncomingServerValidationViewModel(
                oAuthViewModel = PreviewAccountOAuthViewModel(),
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOutgoingValidationScreenK9Preview() {
    K9Theme {
        ServerValidationMainScreen(
            viewModel = FakeOutgoingServerValidationViewModel(
                oAuthViewModel = PreviewAccountOAuthViewModel(),
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOutgoingValidationScreenThunderbirdPreview() {
    ThunderbirdTheme {
        ServerValidationMainScreen(
            viewModel = FakeOutgoingServerValidationViewModel(
                oAuthViewModel = PreviewAccountOAuthViewModel(),
            ),
        )
    }
}
