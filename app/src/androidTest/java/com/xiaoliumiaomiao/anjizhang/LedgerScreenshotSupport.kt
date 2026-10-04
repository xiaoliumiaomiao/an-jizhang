package com.xiaoliumiaomiao.anjizhang
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
internal fun captureLedgerScreen(name:String) {
    val registry=InstrumentationRegistry.getInstrumentation()
    if(InstrumentationRegistry.getArguments().getString("ledgerCi")!="true") return
    fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(registry.uiAutomation.executeShellCommand(command)).bufferedReader().use{it.readText().trim()}
    assertTrue(name.matches(Regex("[a-z0-9-]+")))
    assertEquals("1",shell("getprop ro.kernel.qemu"))
    assertEquals("ci-ledger",shell("getprop ro.boot.qemu.avd_name").ifEmpty{shell("getprop ro.kernel.qemu.avd_name")})
    val directory=File(requireNotNull(registry.targetContext.getExternalFilesDir(null)),"v013-screens")
    check(directory.isDirectory || directory.mkdirs())
    val file=File(directory,"$name.png")
    val bitmap=requireNotNull(registry.uiAutomation.takeScreenshot())
    try { file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))} } finally { bitmap.recycle() }
    val sourcePath=file.absolutePath
    assertTrue(sourcePath.matches(Regex("[a-zA-Z0-9/._-]+")))
    assertTrue("the generated screenshot must contain bytes",file.length()>0)
    val destination="/data/local/tmp/ledger-v013-screens/$name.png"
    // UiAutomation uses Runtime.exec(String); execute one program per call, without shell syntax.
    shell("mkdir -p /data/local/tmp/ledger-v013-screens")
    shell("cp $sourcePath $destination")
    val copiedBytes=shell("stat -c %s $destination").toLong()
    assertEquals("the retrievable screenshot must match the generated PNG size",file.length(),copiedBytes)
}
