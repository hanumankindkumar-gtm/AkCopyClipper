package com.akprojects.copyclipper.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class AutofillAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // React to layout focal shifts or user field selections
    }

    override fun onInterrupt() {
        Log.w("AutofillAccessibility", "Accessibility Service interrupted.")
    }

    /**
     * Simulates direct physical key entries by setting/injecting the character sequence
     * on the active element to bypass restrictive onPaste filters.
     */
    fun performKeystrokeEmulation(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = findFocusedInputNode(rootNode)
        
        if (focusedNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                text
            )
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (success) {
                Log.d("CopyClipperService", "Successfully emulated keystrokes: Injected ${text.length} chars.")
            } else {
                Log.e("CopyClipperService", "Failed to perform text injection action.")
            }
        }
    }

    private fun findFocusedInputNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && (node.className == "android.widget.EditText" || node.isEditable)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val focusedChild = findFocusedInputNode(child)
            if (focusedChild != null) return focusedChild
        }
        return null
    }

    companion object {
        var sharedService: AutofillAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sharedService = this
        Log.i("CopyClipperService", "AutofillAccessibilityService connected successfully!")
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedService = null
    }
}