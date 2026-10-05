package com.xiaoliumiaomiao.anjizhang
import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.ViewTreeObserver
import android.view.inspector.WindowInspector
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
internal fun captureLedgerScreen(name:String) {
    val registry=InstrumentationRegistry.getInstrumentation()
    if(InstrumentationRegistry.getArguments().getString("ledgerCi")!="true") return
    fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(registry.uiAutomation.executeShellCommand(command)).bufferedReader().use{it.readText().trim()}
    assertTrue(name.matches(Regex("[a-z0-9-]+")))
    assertEquals("1",shell("getprop ro.kernel.qemu"))
    assertEquals("ci-ledger",shell("getprop ro.boot.qemu.avd_name").ifEmpty{shell("getprop ro.kernel.qemu.avd_name")})
    val directory=File(requireNotNull(registry.targetContext.getExternalFilesDir(null)),"v013-screens")
    val file=File(directory,"$name.png")
    val sourcePath=file.absolutePath
    assertTrue(sourcePath.matches(Regex("[a-zA-Z0-9/._-]+")))
    assertTrue("frame commit requires the public API 29 or newer",Build.VERSION.SDK_INT>=29)
    val committed=CountDownLatch(1)
    val deadline=SystemClock.elapsedRealtime()+10000L
    var observer:ViewTreeObserver?=null
    var framedRoot:android.view.View?=null
    val focusObservers=mutableListOf<Pair<ViewTreeObserver,ViewTreeObserver.OnWindowFocusChangeListener>>()
    var frameRequested=false
    val callbackFailure=AtomicReference<Throwable?>(null)
    val callback=Runnable { committed.countDown() }
    fun requestFocusedFrame() {
        if(frameRequested) return
        val roots=WindowInspector.getGlobalWindowViews().filter {
            it.isAttachedToWindow && it.isShown && it.hasWindowFocus()
        }
        if(roots.isEmpty()) return
        assertEquals("$name must have one current focused window",1,roots.size)
        val root=roots.single()
        assertTrue("$name frame commit requires hardware rendering",root.isHardwareAccelerated)
        frameRequested=true
        framedRoot=root
        observer=root.viewTreeObserver
        observer!!.registerFrameCommitCallback(callback)
        root.invalidate()
    }
    fun requestFrameSafely() {
        try { requestFocusedFrame() } catch(failure:Throwable) {
            callbackFailure.compareAndSet(null,failure)
            committed.countDown()
        }
    }
    try {
        registry.runOnMainSync {
            val roots=WindowInspector.getGlobalWindowViews().filter {
                it.isAttachedToWindow && it.isShown
            }
            assertTrue("$name must have an attached shown window",roots.isNotEmpty())
            roots.forEach { root ->
                val focusObserver=root.viewTreeObserver
                val listener=ViewTreeObserver.OnWindowFocusChangeListener { focused -> if(focused) requestFrameSafely() }
                focusObserver.addOnWindowFocusChangeListener(listener)
                focusObservers += focusObserver to listener
            }
            // Check after registration too, so an already-focused window cannot miss its frame.
            requestFrameSafely()
        }
        val remaining=deadline-SystemClock.elapsedRealtime()
        val signalled=remaining>0 && committed.await(remaining,TimeUnit.MILLISECONDS)
        // Platform callbacks must report failures on the test thread, not crash the app's UI thread.
        callbackFailure.get()?.let { throw it }
        assertTrue("$name focused frame must commit within one total 10000 ms budget",signalled)
        registry.runOnMainSync {
            val roots=WindowInspector.getGlobalWindowViews().filter { it.isAttachedToWindow && it.isShown && it.hasWindowFocus() }
            assertEquals("$name must have one current focused window",1,roots.size)
            assertSame("$name screenshot must retain the window whose frame committed",framedRoot,roots.single())
            assertTrue("$name frame commit requires hardware rendering",roots.single().isHardwareAccelerated)
        }
    } finally {
        registry.runOnMainSync {
            focusObservers.forEach { (registered,listener) -> if(registered.isAlive) registered.removeOnWindowFocusChangeListener(listener) }
            observer?.takeIf { it.isAlive }?.unregisterFrameCommitCallback(callback)
        }
    }
    check(directory.isDirectory || directory.mkdirs())
    val bitmap=requireNotNull(registry.uiAutomation.takeScreenshot())
    try { file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))} } finally { bitmap.recycle() }
    assertTrue("the generated screenshot must contain bytes",file.length()>0)
    val destination="/data/local/tmp/ledger-v013-screens/$name.png"
    // UiAutomation uses Runtime.exec(String); execute one program per call, without shell syntax.
    shell("mkdir -p /data/local/tmp/ledger-v013-screens")
    shell("cp $sourcePath $destination")
    val copiedBytes=shell("stat -c %s $destination").toLong()
    assertEquals("the retrievable screenshot must match the generated PNG size",file.length(),copiedBytes)
}
