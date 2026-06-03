package com.emoji.overlay.send

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.widget.Toast

object ShareIntentHelper {

    private const val ACTION_SHARE_TARGET_CHOSEN = "com.emoji.overlay.action.SHARE_TARGET_CHOSEN"
    private const val REQUEST_CODE_SHARE_TARGET_CHOSEN = 0x53484152

    private val preferredActivitySuffixes: Map<String, List<String>> = mapOf(
        "com.tencent.mobileqq" to listOf("JumpActivity"),
        "com.tencent.mm" to listOf("ShareImgUI", "SelectConversationUI"),
    )

    private val shareLikePackagePrefixes: Set<String> = setOf(
        "com.ss.android.ugc.aweme",
        "org.telegram.messenger",
    )

    internal data class ShareActivityCandidate(
        val packageName: String,
        val className: String,
    )

    internal fun buildSendIntent(context: Context, payload: SharePayload, mimeType: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "image/*" }
            putExtra(Intent.EXTRA_STREAM, payload.uri)
            clipData = ClipData.newUri(context.contentResolver, "emoji", payload.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    internal fun mimeTypesForQuery(mimeType: String): List<String> {
        val normalized = mimeType.ifBlank { "image/*" }
        if (normalized == "image/*" || !normalized.startsWith("image/")) {
            return listOf(normalized)
        }
        return listOf(normalized, "image/*")
    }

    internal fun resolveShareComponents(
        packageName: String,
        candidates: List<ShareActivityCandidate>,
    ): List<ShareActivityCandidate> {
        val packageCandidates = candidates.filter { it.packageName == packageName }
        if (packageCandidates.isEmpty()) return emptyList()

        val ordered = mutableListOf<ShareActivityCandidate>()
        val preferredSuffixes = preferredActivitySuffixes[packageName].orEmpty()
        for (suffix in preferredSuffixes) {
            ordered.addAll(
                packageCandidates.filter {
                    it.className.endsWith(suffix) || it.className.contains(".$suffix")
                }
            )
        }

        if (shouldPreferShareLikeActivity(packageName)) {
            ordered.addAll(packageCandidates.filter { isShareLikeActivity(it.className) })
        }

        ordered.addAll(packageCandidates.filter { !isLauncherLikeActivity(it.className) })
        ordered.addAll(packageCandidates)

        return ordered.distinctBy { "${it.packageName}/${it.className}" }
    }

    internal fun resolveShareComponent(
        packageName: String,
        candidates: List<ShareActivityCandidate>,
    ): ShareActivityCandidate? = resolveShareComponents(packageName, candidates).firstOrNull()

    private fun shouldPreferShareLikeActivity(packageName: String): Boolean {
        return shareLikePackagePrefixes.any { packageName == it || packageName.startsWith("$it.") }
    }

    private fun isShareLikeActivity(className: String): Boolean {
        return className.contains("Share", ignoreCase = true)
    }

    private fun isLauncherLikeActivity(className: String): Boolean {
        return className.contains("Launcher", ignoreCase = true) ||
            className.endsWith("MainActivity") ||
            className.contains("SplashActivity", ignoreCase = true)
    }

    private fun ResolveInfo.toCandidate(): ShareActivityCandidate? {
        val activityInfo = activityInfo ?: return null
        return ShareActivityCandidate(
            packageName = activityInfo.packageName,
            className = activityInfo.name,
        )
    }

    fun shareToPackage(context: Context, payload: SharePayload, packageName: String) {
        val pm = context.packageManager
        val launchMimeType = payload.mimeType.ifBlank { "image/*" }
        var components: List<ShareActivityCandidate> = emptyList()
        var sendIntent: Intent? = null

        for (queryMimeType in mimeTypesForQuery(payload.mimeType)) {
            val queryIntent = buildSendIntent(context, payload, queryMimeType)
            val candidates = pm.queryIntentActivities(queryIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.toCandidate() }
            components = resolveShareComponents(packageName, candidates)
            if (components.isNotEmpty()) {
                sendIntent = buildSendIntent(context, payload, launchMimeType)
                break
            }
        }

        if (sendIntent == null || components.isEmpty()) {
            showShareFailureToast(context, packageName)
            return
        }

        context.grantUriPermission(
            packageName,
            payload.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        for (candidate in components) {
            val launchIntent = Intent(sendIntent).apply {
                component = ComponentName(candidate.packageName, candidate.className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(launchIntent)
                return
            } catch (_: ActivityNotFoundException) {
                continue
            }
        }

        showShareFailureToast(context, packageName)
    }

    fun shareWithSystemChooser(context: Context, payload: SharePayload) {
        val sendIntent = buildSendIntent(context, payload, payload.mimeType.ifBlank { "image/*" })

        val callbackIntent = Intent(context, ShareTargetChosenReceiver::class.java).apply {
            action = ACTION_SHARE_TARGET_CHOSEN
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or chooserCallbackPendingIntentFlags()
        val callbackPendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_CODE_SHARE_TARGET_CHOSEN,
            callbackIntent,
            pendingFlags
        )

        val chooser = Intent.createChooser(sendIntent, payload.title, callbackPendingIntent.intentSender).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun showShareFailureToast(context: Context, packageName: String) {
        val label = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
        Toast.makeText(
            context,
            "无法打开${label}分享，请使用更多应用",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun chooserCallbackPendingIntentFlags(): Int {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PendingIntent.FLAG_MUTABLE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> PendingIntent.FLAG_IMMUTABLE
            else -> 0
        }
    }
}
