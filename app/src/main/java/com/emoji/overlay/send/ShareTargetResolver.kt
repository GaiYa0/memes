package com.emoji.overlay.send

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

data class ShareTargetDefinition(
    val label: String,
    val packageNames: List<String>
)

data class ResolvedShareTarget(
    val label: String,
    val packageName: String
)

object ShareTargetResolver {
    val PRIORITY_TARGETS: List<ShareTargetDefinition> = listOf(
        ShareTargetDefinition("QQ", listOf("com.tencent.mobileqq")),
        ShareTargetDefinition("微信", listOf("com.tencent.mm")),
        ShareTargetDefinition(
            "抖音",
            listOf("com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.lite")
        ),
        ShareTargetDefinition(
            "Telegram",
            listOf("org.telegram.messenger", "org.telegram.messenger.web")
        )
    )

    private val ALL_PRIORITY_PACKAGES: Set<String> = PRIORITY_TARGETS
        .flatMap { it.packageNames }
        .toSet()

    fun isPriorityPackage(packageName: String): Boolean = packageName in ALL_PRIORITY_PACKAGES

    fun isResolvablePackage(context: Context, packageName: String): Boolean {
        return runCatching {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }

    fun queryShareHandlerPackages(
        context: Context,
        mimeType: String,
        streamUri: Uri? = null
    ): Set<String> {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "image/*" }
            if (streamUri != null) {
                putExtra(Intent.EXTRA_STREAM, streamUri)
                clipData = ClipData.newUri(context.contentResolver, "emoji", streamUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val activities = context.packageManager.queryIntentActivities(sendIntent, flags)
        return activities.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    fun resolveInstalledTargets(
        installedPackages: Set<String>,
        targets: List<ShareTargetDefinition> = PRIORITY_TARGETS
    ): List<ResolvedShareTarget> {
        return targets.mapNotNull { target ->
            val packageName = target.packageNames.firstOrNull { it in installedPackages }
            packageName?.let { ResolvedShareTarget(label = target.label, packageName = it) }
        }
    }

    fun resolveDisplayTargets(
        shareHandlers: Set<String>,
        customSlot4Package: String?,
        labelResolver: (String) -> String = { it },
        packageAvailable: (String) -> Boolean = { true }
    ): List<ResolvedShareTarget> {
        val result = mutableListOf<ResolvedShareTarget>()

        for (index in 0 until PRIORITY_TARGETS.lastIndex) {
            resolveSlot(PRIORITY_TARGETS[index], shareHandlers)?.let { result.add(it) }
        }

        val slot4 = resolveSlot4(shareHandlers, customSlot4Package, labelResolver, packageAvailable)
        slot4?.let { result.add(it) }

        return result
    }

    private fun resolveSlot4(
        shareHandlers: Set<String>,
        customSlot4Package: String?,
        labelResolver: (String) -> String,
        packageAvailable: (String) -> Boolean
    ): ResolvedShareTarget? {
        customSlot4Package?.takeIf { it.isNotBlank() }?.let { pkg ->
            if (pkg in shareHandlers || packageAvailable(pkg)) {
                return ResolvedShareTarget(label = labelResolver(pkg), packageName = pkg)
            }
        }
        return resolveSlot(PRIORITY_TARGETS.last(), shareHandlers)
    }

    private fun resolveSlot(
        definition: ShareTargetDefinition,
        shareHandlers: Set<String>
    ): ResolvedShareTarget? {
        val packageName = definition.packageNames.firstOrNull { it in shareHandlers } ?: return null
        return ResolvedShareTarget(label = definition.label, packageName = packageName)
    }

    fun resolveFirstAvailablePackage(
        packageNames: List<String>,
        installedPackages: Set<String>
    ): String? = packageNames.firstOrNull { it in installedPackages }

    fun resolveDisplayTargetsForContext(
        context: Context,
        mimeType: String,
        customSlot4Package: String?,
        streamUri: Uri? = null
    ): List<ResolvedShareTarget> {
        val shareHandlers = queryShareHandlerPackages(context, mimeType, streamUri)
        val labelResolver: (String) -> String = { packageName ->
            runCatching {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            }.getOrDefault(packageName)
        }
        return resolveDisplayTargets(
            shareHandlers = shareHandlers,
            customSlot4Package = customSlot4Package,
            labelResolver = labelResolver,
            packageAvailable = { isResolvablePackage(context, it) }
        )
    }
}
