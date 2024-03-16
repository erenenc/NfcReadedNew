package com.example.nfcreadednew

import android.app.Application
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfcreader.NFCStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    companion object {
        private val TAG = "mTAGViewModel"
        private const val prefix = "android.nfc.tech."
    }

    private val _liveNFC: MutableStateFlow<NFCStatus?> = MutableStateFlow(null)
    val liveNFC = _liveNFC.asStateFlow()

    private val _liveMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val liveMessage = _liveMessage.asStateFlow()

    fun getNFCFlags(): Int {
        return NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE //or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    }

    fun getExtras(): Bundle {
        val options: Bundle = Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 30000)
        return options
    }


    //region NFC Methods
    fun onCheckNFC(isChecked: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "onCheckNFC(${isChecked})")
            if (isChecked) {
                postNFCStatus(NFCStatus.Tap)
            } else {
                postNFCStatus(NFCStatus.NoOperation)
            }
        }
    }

    private fun postNFCStatus(status: NFCStatus) {
        Log.d(TAG, "postNFCStatus(${status})")

        if (status == NFCStatus.Tap) {
            if (NFCManager.isSupportedAndEnabled(context)) {
                _liveNFC.update { status }
                _liveMessage.update { "Please Tap Now!" }
            } else if (NFCManager.isNotEnabled(context)) {
                _liveNFC.update { NFCStatus.NotEnabled }
                _liveMessage.update { "Please Enable your NFC!" }
            } else if (NFCManager.isNotSupported(context)) {
                _liveNFC.update { NFCStatus.NotSupported }
                _liveMessage.update { "NFC Not Supported!" }
            }
        } else {
            _liveNFC.update { status }
            _liveMessage.update { "NFC is disabled by user" }
        }
    }

    /*fun readTag(tag: Tag?) {
        // bunun yerine activity onTagDiscovered içindeki kodlar kullanılacak
        viewModelScope.launch {
            Log.d(TAG, "readTag(${tag} ${tag?.techList})")
            postNFCStatus(NFCStatus.Process)
            val stringBuilder: StringBuilder = StringBuilder()
            val id: ByteArray? = tag?.id
            stringBuilder.append("Tag ID (hex): ${getHex(id!!)} \n")
            stringBuilder.append("Tag ID (dec): ${getDec(id)} \n")
            stringBuilder.append("Tag ID (reversed): ${getReversed(id)} \n")
            stringBuilder.append("Technologies: ")
            tag.techList.forEach { tech ->
                stringBuilder.append(tech.substring(prefix.length))
                stringBuilder.append(", ")
            }
            stringBuilder.delete(stringBuilder.length - 2, stringBuilder.length)
            tag.techList.forEach { tech ->
                if (tech.equals(MifareClassic::class.java.name)) {
                    stringBuilder.append('\n')
                    val newTag = cleanupTag(tag)
                    val mifareTag: MifareClassic = MifareClassic.get(newTag)
                    val type: String
                    if (mifareTag.type == MifareClassic.TYPE_CLASSIC) type = "Classic"
                    else if (mifareTag.type == MifareClassic.TYPE_PLUS) type = "Plus"
                    else if (mifareTag.type == MifareClassic.TYPE_PRO) type = "Pro"
                    else type = "Unknown"
                    stringBuilder.append("Mifare Classic type: $type \n")
                    stringBuilder.append("Mifare size: ${mifareTag.size} bytes \n")
                    stringBuilder.append("Mifare sectors: ${mifareTag.sectorCount} \n")
                    stringBuilder.append("Mifare blocks: ${mifareTag.blockCount}")
                    stringBuilder.append("Mifare type: ${mifareTag.type}")
                }
                if (tech.equals(MifareUltralight::class.java.name)) {
                    stringBuilder.append('\n')
                    val newTag = cleanupTag(tag)
                    val mifareUlTag: MifareUltralight = MifareUltralight.get(newTag)
                    val type: String
                    if (mifareUlTag.type == MifareUltralight.TYPE_ULTRALIGHT) type = "Ultralight"
                    else if (mifareUlTag.type == MifareUltralight.TYPE_ULTRALIGHT_C) type =
                        "Ultralight C"
                    else type = "Unkown"
                    stringBuilder.append("Mifare Ultralight type: ");
                    stringBuilder.append(type)
                }
            }
            Log.d(TAG, "Datum: $stringBuilder")
            Log.d(ContentValues.TAG, "dumpTagData Return \n $stringBuilder")
            postNFCStatus(NFCStatus.Read)
            _liveTag.update { "${getDateTimeNow()} \n ${stringBuilder}" }
        }
    }

    private fun getDateTimeNow(): String {
        Log.d(TAG, "getDateTimeNow()")
        val timeFormat: DateFormat = SimpleDateFormat.getDateTimeInstance()
        val now: Date = Date()
        Log.d(ContentValues.TAG, "getDateTimeNow() Return ${timeFormat.format(now)}")
        return timeFormat.format(now)
    }

    private fun getHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices.reversed()) {
            val b: Int = bytes[i].and(0xff.toByte()).toInt()
            if (b < 0x10) sb.append('0')
            sb.append(Integer.toHexString(b))
            if (i > 0)
                sb.append(" ")
        }
        return sb.toString()
    }

    private fun getDec(bytes: ByteArray): Long {
        Log.d(TAG, "getDec()")
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.indices) {
            val value: Long = bytes[i].and(0xffL.toByte()).toLong()
            result += value * factor
            factor *= 256L
        }
        return result
    }

    private fun getReversed(bytes: ByteArray): Long {
        Log.d(TAG, "getReversed()")
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.indices.reversed()) {
            val value = bytes[i].and(0xffL.toByte()).toLong()
            result += value * factor
            factor *= 256L
        }
        return result
    }

    private fun cleanupTag(oTag: Tag?): Tag? {
        if (oTag == null) return null
        val sTechList = oTag.techList
        val oParcel = Parcel.obtain()
        oTag.writeToParcel(oParcel, 0)
        oParcel.setDataPosition(0)
        val len = oParcel.readInt()
        var id: ByteArray? = null
        if (len >= 0) {
            id = ByteArray(len)
            oParcel.readByteArray(id)
        }
        val oTechList = IntArray(oParcel.readInt())
        oParcel.readIntArray(oTechList)
        val oTechExtras = oParcel.createTypedArray(Bundle.CREATOR)
        val serviceHandle = oParcel.readInt()
        val isMock = oParcel.readInt()
        val tagService: IBinder?
        tagService = if (isMock == 0) {
            oParcel.readStrongBinder()
        } else {
            null
        }
        oParcel.recycle()
        var nfca_idx = -1
        var mc_idx = -1
        var oSak: Short = 0
        var nSak: Short = 0
        for (idx in sTechList.indices) {
            if (sTechList[idx] == NfcA::class.java.name) {
                if (nfca_idx == -1) {
                    nfca_idx = idx
                    if (oTechExtras!![idx] != null
                        && oTechExtras!![idx]!!.containsKey("sak")
                    ) {
                        oSak = oTechExtras!![idx]!!.getShort("sak")
                        nSak = oSak
                    }
                } else {
                    if (oTechExtras!![idx] != null
                        && oTechExtras!![idx]!!.containsKey("sak")
                    ) {
                        nSak =
                            (nSak.toInt() or oTechExtras!![idx]!!.getShort("sak")
                                .toInt()).toShort()
                    }
                }
            } else if (sTechList[idx] == MifareClassic::class.java.name) {
                mc_idx = idx
            }
        }
        var modified = false
        if (oSak != nSak) {
            oTechExtras!![nfca_idx]!!.putShort("sak", nSak)
            modified = true
        }
        if (nfca_idx != -1 && mc_idx != -1 && oTechExtras!![mc_idx] == null) {
            oTechExtras!![mc_idx] = oTechExtras!![nfca_idx]
            modified = true
        }
        if (!modified) {
            return oTag
        }
        val nParcel = Parcel.obtain()
        nParcel.writeInt(id!!.size)
        nParcel.writeByteArray(id)
        nParcel.writeInt(oTechList.size)
        nParcel.writeIntArray(oTechList)
        nParcel.writeTypedArray(oTechExtras, 0)
        nParcel.writeInt(serviceHandle)
        nParcel.writeInt(isMock)
        if (isMock == 0) {
            nParcel.writeStrongBinder(tagService)
        }
        nParcel.setDataPosition(0)
        val nTag = Tag.CREATOR.createFromParcel(nParcel)
        nParcel.recycle()
        return nTag
    }*/
}