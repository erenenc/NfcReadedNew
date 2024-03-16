package com.example.nfcreadednew

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nfcreadednew.databinding.ActivityMainBinding
import com.example.nfcreader.NFCStatus
import com.github.devnied.emvnfccard.parser.EmvTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        private val TAG = "mTAGActivity"
    }

    lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        subscribeToObservers()

        binding.btnNfc.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView == binding.btnNfc) {
                viewModel.onCheckNFC(isChecked)
            }
        }

    }

    private fun subscribeToObservers() {

        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.liveNFC.collect { nfcStatus ->
                    Log.d(TAG, "observeNFCStatus $nfcStatus")
                    if (nfcStatus == NFCStatus.NoOperation) NFCManager.disableReaderMode(
                        this@MainActivity,
                        this@MainActivity
                    )
                    else if (nfcStatus == NFCStatus.Tap) NFCManager.enableReaderMode(
                        this@MainActivity,
                        this@MainActivity,
                        this@MainActivity,
                        viewModel.getNFCFlags(),
                        viewModel.getExtras()
                    )
                }
            }
        }

        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.liveTag.collect { tag ->
                    Log.d(TAG, "observeTag $tag")
                    binding.tvNfc.text = tag
                }
            }
        }

        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.liveToast.collect { message ->
                    Log.d(TAG, "observeToast $message")
                    if (message != null) {
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onTagDiscovered(tag: Tag?) {
        readNfcTagInfo(tag)
        //viewModel.readTag(tag)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readNfcTagInfo(tag: Tag?) {
        Log.d(TAG, "onTagDiscovered called")
        val isoDep: IsoDep?
        try {
            isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(
                    VibrationEffect.createOneShot(
                        150,
                        10
                    )
                )
            }
            isoDep.connect()
            val provider = PcscProvider()
            provider.setmTagCom(isoDep)
            val config = EmvTemplate.Config()
                .setContactLess(true)
                .setReadAllAids(true)
                .setReadTransactions(true)
                .setRemoveDefaultParsers(false)
                .setReadAt(true)
            val parser = EmvTemplate.Builder()
                .setProvider(provider)
                .setConfig(config)
                .build()

            val card = parser.readEmvCard()
            val type = card.type
            val applicationLabel = card.applications[0].applicationLabel
            val track1 = card.track1
            val track2 = card.track2
            val cardNumber = card.cardNumber
            val expireDate = card.expireDate
            var date = LocalDate.of(1999, 12, 31)
            if (expireDate != null) {
                date = expireDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }


            //Log.d(TAG, "onTagDiscovered called 1 $card")
            CoroutineScope(Dispatchers.Main).launch {
                binding.tvNfcCardNumber.text = cardNumber
                Log.d(TAG, "cardNumber: $cardNumber")

                Log.d(TAG, "expire Date: ${date.toString()}")
                binding.tvNfcExpireDate.text = date.toString()

                Log.d(TAG, "type: $type")
                Log.d(TAG, "applicationLabel: $applicationLabel")
                Log.d(TAG, "track1: $track1")
                Log.d(TAG, "track2: $track2")
            }

            try {
                isoDep.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: IOException) {
            Log.d(TAG, "onTagDiscovered: catch 1 called e: $e")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.d(TAG, "onTagDiscovered: catch 2 called e: $e")
            e.printStackTrace()
        }
    }
}