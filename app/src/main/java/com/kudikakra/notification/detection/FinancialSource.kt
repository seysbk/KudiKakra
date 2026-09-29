package com.kudikakra.notification.detection

/**
 * Each entry represents a supported financial provider.
 *
 * [packageNames] — the Android package names known to belong to this provider.
 *   A single provider may ship multiple apps (e.g. the main app + a lite variant).
 *
 * [displayName] — shown in the user-facing Settings screen so the user can
 *   choose which services KudiKakra should monitor.
 *
 * To add a new provider in a future phase, add a new enum entry here.
 * No other detection logic needs to change.
 */
enum class FinancialSource(
    val packageNames: List<String>,
    val displayName: String,
) {
    MTN_MOMO(
        packageNames = listOf(
            "com.mtn.momo",
            "com.mtn.mtnmomopro",
            "com.mtn.mtnmomo",
        ),
        displayName = "MTN MoMo",
    ),
    GCB(
        packageNames = listOf(
            "com.gcb.gcbmobile",
            "com.gcb.retail",
        ),
        displayName = "GCB Bank",
    ),
    ;

    companion object {
        /**
         * Returns the [FinancialSource] whose [packageNames] contains [packageName],
         * or null if the package is not associated with any known provider.
         */
        fun fromPackageName(packageName: String): FinancialSource? =
            entries.firstOrNull { source -> source.packageNames.contains(packageName) }
    }
}
