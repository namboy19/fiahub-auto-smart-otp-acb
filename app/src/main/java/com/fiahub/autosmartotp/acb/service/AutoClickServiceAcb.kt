package com.fiahub.autosmartotp.acb.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.fiahub.autosmartotp.acb.MainActivity
import com.fiahub.autosmartotp.acb.logd
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


var autoClickServiceAcb: AutoClickServiceAcb? = null

class AutoClickServiceAcb : AccessibilityService(), CoroutineScope {

    override fun onInterrupt() {
        // NO-OP
    }

    private var isStarted = false
    private var unlockOtpPin: String? = null
    private var onGeneratedOtp: ((String) -> Unit)? = null
    private var currentOtp = ""

    private var isLoadedLoginScreen = false
    private var isLoadedHomeScreen = false

    private val allNodesInfo = mutableListOf<AccessibilityNodeInfo>()

    companion object {
        private const val DELAY_TIME_FOR_RENDER_SCREEN = 300L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d("nam", event.toString())
        handleScreenChanged()

        allNodesInfo.clear()
        getNodeInfo(rootInActiveWindow)
    }

    private fun getNodeInfo(node: AccessibilityNodeInfo?) {

        if (node == null) {
            return
        }

        try {
            allNodesInfo.add(node)

            if (node.childCount > 0) {
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let {
                        getNodeInfo(it)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isLoginScreen(): Boolean {
        //--login screen
        return !rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.mk.acb.safekey:id/pin_txtTut")
            .isNullOrEmpty()
    }

    private fun isHomeScreen(): Boolean {
        return !rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.mk.acb.safekey:id/user_txtAbove")
            .isNullOrEmpty()
    }

    private fun isGenerateOtpScreen(): Boolean {
        return !rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.mk.acb.safekey:id/otp_txtCode")
            .isNullOrEmpty()
    }

    fun startGetOtp(pinUnlockOtp: String,
                    onGeneratedOtp: (String) -> Unit) {

        isStarted = true
        unlockOtpPin = pinUnlockOtp
        this.onGeneratedOtp = onGeneratedOtp

        handleScreenChanged()
    }

    fun stopGetOtp() {
        isStarted = false
        isLoadedLoginScreen = false
        isLoadedHomeScreen = false
    }

    private fun handleScreenChanged() {

        GlobalScope.launch(Dispatchers.Main) {

            if (!isStarted) {
                return@launch
            }

            when {
                isLoginScreen() && !isLoadedLoginScreen -> {
                    isLoadedLoginScreen = true

                    //wait screen fully display
                    delay(DELAY_TIME_FOR_RENDER_SCREEN)

                    unlockOtpPin?.forEachIndexed { index, c ->
                        rootInActiveWindow?.findAccessibilityNodeInfosByText(c.toString())
                            ?.firstOrNull()?.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                    }
                }

                isHomeScreen() && !isLoadedHomeScreen -> {
                    //sendLog("-> home screen")

                    isLoadedHomeScreen = true
                    //wait screen fully display
                    delay(DELAY_TIME_FOR_RENDER_SCREEN)

                    rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.mk.acb.safekey:id/user_txtAbove")
                        ?.firstOrNull()?.let {
                            performClick(it)
                        }
                }

                isGenerateOtpScreen() -> {
                    rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.mk.acb.safekey:id/otp_txtCode")
                        ?.firstOrNull()?.text?.let {

                            val otp = it.toString().filterNot { it.isWhitespace() }

                            if (otp.isNotEmpty() && otp != currentOtp) {
                                currentOtp=otp
                                onGeneratedOtp?.invoke(it.toString().filterNot { it.isWhitespace() })
                            }
                        }
                }

                else -> {

                }
            }
        }
    }

    private fun performClick(node: AccessibilityNodeInfo) {
        //-- due to the item is wrapper in another view, so only the parents receive click event
        // force click all parent of item
        node.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
        var currentNode = node
        while (currentNode.parent != null) {
            currentNode.parent.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            currentNode = currentNode.parent
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        "onServiceConnected".logd()
        autoClickServiceAcb = this
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onUnbind(intent: Intent?): Boolean {
        autoClickServiceAcb = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        autoClickServiceAcb = null
        super.onDestroy()
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main
}