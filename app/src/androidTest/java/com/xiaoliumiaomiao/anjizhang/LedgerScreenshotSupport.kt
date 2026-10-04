package com.xiaoliumiaomiao.anjizhang
import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.ViewTreeObserver
import android.view.inspector.WindowInspector
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
    var observer:ViewTreeObserver?=null
    val callback=Runnable { committed.countDown() }
    try {
        registry.runOnMainSync {
            val roots=WindowInspector.getGlobalWindowViews().filter {
                it.isAttachedToWindow && it.isShown && it.hasWindowFocus()
            }
            assertEquals("$name must have one current focused window",1,roots.size)
            val root=roots.single()
            assertTrue("$name frame commit requires hardware rendering",root.isHardwareAccelerated)
            observer=root.viewTreeObserver
            observer!!.registerFrameCommitCallback(callback)
            root.invalidate()
        }
        assertTrue("$name frame must commit within 10000 ms",committed.await(10,TimeUnit.SECONDS))
    } finally {
        registry.runOnMainSync {
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
