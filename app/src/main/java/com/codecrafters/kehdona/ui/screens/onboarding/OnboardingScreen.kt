package com.codecrafters.kehdona.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codecrafters.kehdona.data.onboarding.OnboardingContent
import com.codecrafters.kehdona.model.OnboardingPage
import com.codecrafters.kehdona.ui.components.AmbientBackground
import com.codecrafters.kehdona.ui.components.Badge
import com.codecrafters.kehdona.ui.components.BadgeTone
import com.codecrafters.kehdona.ui.components.KehDoNaLogo
import com.codecrafters.kehdona.ui.components.PageIndicator
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.QuietButton
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import kotlinx.coroutines.launch

private val WideLayoutBreakpoint = 600.dp

/** Stateful entry point: wires [OnboardingViewModel] to the screen. */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onFinished()
    }

    OnboardingScreen(
        pages = state.pages,
        isSaving = state.isSaving,
        onFinish = viewModel::complete,
    )
}

@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage>,
    isSaving: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    AmbientBackground(modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val wide = maxWidth >= WideLayoutBreakpoint
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 8.dp)
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    KehDoNaLogo(markSize = 24.dp, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    if (!isLastPage) QuietButton(text = "Skip", onClick = onFinish)
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { index ->
                    OnboardingPageContent(page = pages[index], wide = wide)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PageIndicator(pageCount = pages.size, currentPage = pagerState.currentPage)
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        text = if (isLastPage) "Continue" else "Next",
                        onClick = {
                            if (isLastPage) {
                                onFinish()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        loading = isSaving,
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "KehDoNa is in early development — these features are on the way.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSubtle,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, wide: Boolean) {
    if (wide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                OnboardingIllustration(page.illustration)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                PageText(page, TextAlign.Start)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
                OnboardingIllustration(page.illustration)
            }
            Spacer(Modifier.height(32.dp))
            Column(Modifier.widthIn(max = 480.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PageText(page, TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PageText(page: OnboardingPage, textAlign: TextAlign) {
    Badge(text = page.eyebrow, tone = BadgeTone.Rose)
    Spacer(Modifier.height(16.dp))
    Text(
        text = page.title,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = textAlign,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = page.body,
        style = MaterialTheme.typography.bodyLarge,
        color = InkMuted,
        textAlign = textAlign,
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingScreenPreview() {
    KehDoNaTheme { OnboardingScreen(pages = OnboardingContent.pages, isSaving = false, onFinish = {}) }
}
