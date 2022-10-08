package com.denis.librereader

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.denis.librereader.databinding.ActivityMainBinding
import com.denis.librereader.ui.main.MainFragment
import com.denis.repository.nfc.NfcReader
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.*
import kotlin.experimental.and

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        private const val START_INDEX = 0
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
        private const val BITMASK = 0x0FFF
        private const val NFC_LOG_TAG = "socialdiabetes"
        var lectura: String? = null

        var printer: String? = null

        var currentGlucose = 0F
    }

    private var nfcAdapter: NfcAdapter? = null

    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment).navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter?.isEnabled == false) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_SHORT).show()
        }
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        setupForegroundDispatch(this, nfcAdapter)
    }

    override fun onPause() {
        stopForegroundDispatch(this, nfcAdapter)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        if (NfcAdapter.ACTION_TECH_DISCOVERED == action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            NfcReader.onTagDiscovered(tag)
            val nfcvTag = NfcV.get(tag)
            Log.d("socialdiabetes", "Enter NdefReaderTask: $nfcvTag")
            Log.d("socialdiabetes", "Tag ID: " + tag?.id)
            try {
                nfcvTag?.connect()
            } catch (e: IOException) {
//            Toast.makeText(Context()!!, "Connection error", Toast.LENGTH_LONG)
            }
            lectura = ""
            val bloques = Array(40) { ByteArray(8) }
            val allBlocks = ByteArray(40 * 8)
            Log.d(
                "socialdiabetes",
                "---------------------------------------------------------------"
            )
            try {

                // Get system information (0x2B)
                var cmd = byteArrayOf(0x00.toByte(), 0x2B.toByte())
                var systeminfo = nfcvTag.transceive(cmd)
                printer = systeminfo.toString()
                systeminfo = Arrays.copyOfRange(systeminfo, 2, systeminfo.size - 1)
                val memorySize = byteArrayOf(systeminfo[6], systeminfo[5])
                Log.d(
                    "socialdiabetes",
                    "Memory Size: " + bytesToHex(memorySize) + " / " + bytesToHex(
                        memorySize
                    ).trim { it <= ' ' }
                        .toInt(16))
                val blocks = byteArrayOf(systeminfo[8])
                Log.d(
                    "socialdiabetes",
                    "blocks: " + bytesToHex(blocks) + " / " + bytesToHex(blocks)
                        .trim { it <= ' ' }
                        .toInt(16))
                val totalBlocks = bytesToHex(blocks).trim { it <= ' ' }.toInt(16)
                for (i in 3..40) {
                    cmd = byteArrayOf(0x00.toByte(), 0x20.toByte(), i.toByte())
                    var oneBlock = nfcvTag.transceive(cmd)
                    Log.d(
                        "socialdiabetes",
                        "userdata: " + oneBlock.toString() + " - " + oneBlock.size
                    )
                    oneBlock = Arrays.copyOfRange(oneBlock, 1, oneBlock.size)
                    bloques[i - 3] = Arrays.copyOf(oneBlock, 8)
                    Log.d("socialdiabetes", "userdata HEX: " + bytesToHex(oneBlock))
                    lectura = """
                        ${lectura}${bytesToHex(oneBlock)}
                        
                        """.trimIndent()
                }
                var s = ""
                for (i in 0..39) {
                    Log.d("socialdiabetes", bytesToHex(bloques[i]))
                    s = s + bytesToHex(bloques[i])
                }
                Log.d("socialdiabetes", "S: $s")
                Log.d("socialdiabetes", "Next read: " + s.substring(4, 6))
                val current = s.substring(4, 6).toInt(16)
                Log.d("socialdiabetes", "Next read: $current")
                Log.d("socialdiabetes", "Next historic read " + s.substring(6, 8))
                val bloque1 = arrayOfNulls<String>(16)
                val bloque2 = arrayOfNulls<String>(32)
                Log.d("socialdiabetes", "--------------------------------------------------")
                var ii = 0

                var index = 8
                while (index < 8 + 15 * 12) {
                    Log.d("socialdiabetes", s.substring(index, index + 12))
                    bloque1[ii] = s.substring(index, index + 12)
                    val g = s.substring(index + 2, index + 4) + s.substring(index, index + 2)
                    if (current == ii) {
                        currentGlucose = glucoseReading(g.toInt(16))
                    }
                    ii++
                    index += 12
                }

                lectura = lectura + "Current approximate glucose " + currentGlucose
                Log.d("socialdiabetes", "Current approximate glucose ${currentGlucose}")
                Log.d("socialdiabetes", "--------------------------------------------------")
                ii = 0
                var i = 188
                while (i < 188 + 31 * 12) {
                    Log.d("socialdiabetes", s.substring(i, i + 12))
                    bloque2[ii] = s.substring(i, i + 12)
                    ii++
                    i += 12
                }
                Log.d("socialdiabetes", "--------------------------------------------------")
            } catch (e: IOException) {
                return
            }
            try {
                nfcvTag?.close()
            } catch (e: IOException) {
                return
            }
            navController.navigate(R.id.mainFragment, MainFragment.saveGlucose(currentGlucose.toString(), printer.toString()))
        }
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v: Int = (bytes[j] and 0xFF.toByte()).toInt()
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }

    fun glucoseReading(value: Int): Float {
        // ((0x4531 & 0xFFF) / 6) - 37;
        return java.lang.Float.valueOf(java.lang.Float.valueOf(((value and BITMASK) / 6).toFloat()) - 37)
    }

    @SuppressLint("InlinedApi")
    fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
        val intent = Intent(activity.applicationContext, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        // Notice that this is the same filter as in our manifest.
        val filters = arrayOfNulls<IntentFilter>(1)
        filters[START_INDEX] = IntentFilter()
        filters[START_INDEX]?.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filters[START_INDEX]?.addCategory(Intent.CATEGORY_DEFAULT)

        adapter?.enableForegroundDispatch(
            activity,
            PendingIntent.getActivity(activity.applicationContext, 0, intent, FLAG_MUTABLE),
            filters,
            arrayOf<Array<String>>()
        )
    }

    private fun stopForegroundDispatch(activity: Activity?, adapter: NfcAdapter?) {
        adapter?.disableForegroundDispatch(activity)
    }
}