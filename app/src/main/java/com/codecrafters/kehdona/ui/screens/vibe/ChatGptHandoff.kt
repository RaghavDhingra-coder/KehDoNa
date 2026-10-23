package com.codecrafters.kehdona.ui.screens.vibe

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.codecrafters.kehdona.domain.PersonalityPrompt

/** Copies the personality prompt and opens ChatGPT: the app if installed, otherwise the website. */
object ChatGptHandoff {
    /** Declared in the manifest's <queries> so it's visible on Android 11+. */
    const val AppPackage = "com.openai.chatgpt"
    const val WebUrl = "https://chatgpt.com/"

    fun copyPrompt(context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("KehDoNa personality prompt", PersonalityPrompt.text))
    }

    fun open(context: Context): ChatGptTarget {
        context.packageManager.getLaunchIntentForPackage(AppPackage)?.let { launch ->
            if (start(context, launch)) return ChatGptTarget.App
        }
        val browser = Intent(Intent.ACTION_VIEW, WebUrl.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        return if (start(context, browser)) ChatGptTarget.Browser else ChatGptTarget.None
    }

    /** Copies first, so the prompt is ready to paste the moment ChatGPT opens. */
    fun copyAndOpen(context: Context): ChatGptTarget {
        copyPrompt(context)
        return open(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
