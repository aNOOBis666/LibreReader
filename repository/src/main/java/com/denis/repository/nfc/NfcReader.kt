package com.denis.repository.nfc

import android.content.Context
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Singleton
import kotlin.experimental.and

@Singleton
object NfcReader : NfcAdapter.ReaderCallback {



    override fun onTagDiscovered(tag: Tag?) {

    }

//    private fun connectTag(tag: NfcV?): NfcV? {
//
//    }

//    private fun disconnectTag(tag: NfcV?) {
//
//    }



}