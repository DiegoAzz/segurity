package com.familyguard.child.child_flutter

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.NonNull
import com.familyguard.child.child_flutter.engine.PolicyEngine
import com.familyguard.child.child_flutter.engine.PolicySnapshot
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.familyguard.child/enforcement"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            val policyEngine = PolicyEngine.getInstance(applicationContext)

            when (call.method) {
                "syncPolicy" -> {
                    val version = call.argument<Int>("version") ?: 1
                    val instantLock = call.argument<Boolean>("instantLockActive") ?: false
                    val dailyLimit = call.argument<Int>("dailyLimitMinutes") ?: 120
                    val dailyUsage = call.argument<Int>("dailyUsageMinutes") ?: 0
                    val bedtime = call.argument<Boolean>("bedtimeActive") ?: false
                    val school = call.argument<Boolean>("schoolActive") ?: false
                    val blockedList = call.argument<List<String>>("blockedPackages") ?: emptyList()

                    val snapshot = PolicySnapshot(
                        version = version,
                        instantLockActive = instantLock,
                        dailyLimitMinutes = dailyLimit,
                        dailyUsageMinutes = dailyUsage,
                        bedtimeActive = bedtime,
                        schoolActive = school,
                        blockedPackages = blockedList.toSet()
                    )

                    val applied = policyEngine.updatePolicy(snapshot)
                    result.success(applied)
                }

                "evaluateApp" -> {
                    val pkg = call.argument<String>("packageName") ?: ""
                    val decision = policyEngine.evaluateApp(pkg)
                    val map = mapOf(
                        "packageName" to decision.packageName,
                        "isBlocked" to decision.isBlocked,
                        "reason" to decision.reason.name,
                        "policyVersion" to decision.policyVersion
                    )
                    result.success(map)
                }

                "diagnosePermissions" -> {
                    val diag = mapOf(
                        "hasUsageStats" to hasUsageStatsPermission(),
                        "hasOverlay" to hasOverlayPermission(),
                        "isBatteryIgnored" to isBatteryOptimizationIgnored()
                    )
                    result.success(diag)
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
        } else {
            true
        }
    }
}
