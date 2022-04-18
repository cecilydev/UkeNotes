package edu.temple.ukenotes

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.psambit9791.jdsp.transform.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Math.abs


class MainActivity : AppCompatActivity() {

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val samplingRate = 44100 /* in Hz*/
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(samplingRate, channelConfig, audioFormat)
    var recorder: AudioRecord? = null
    var isRecording = false
    var recordingThread: Thread? = null
    var audioByteArray: ByteArray? = null
    //val audioShortArray = ShortArray(bufferSize/4)
    val newArray = mutableListOf<Short>()
    lateinit var text: TextView
    var buffCount = 0
    private lateinit var preferences: SharedPreferences
    private val internalFilename = "my_file"
    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ), 1
            )
        }

        preferences = getPreferences(MODE_PRIVATE)
        file = File(filesDir, internalFilename)
        Log.d("BUFFER SIZE", bufferSize.toString())
        text = findViewById(R.id.textView)
        //recorder = AudioRecord(audioSource, samplingRate, channelConfig, audioFormat, bufferSize)

        findViewById<Button>(R.id.recordButton).setOnClickListener {
            text.text = ""
            recorder = AudioRecord(audioSource, samplingRate, channelConfig, audioFormat, bufferSize)
            isRecording = true
            recorder?.startRecording()
            recordingThread = Thread({ writeToVar() }, "AudioRecorder Thread")
            recordingThread?.start()
        }

       findViewById<Button>(R.id.stopButton).setOnClickListener {
            isRecording = false
            recorder?.stop()
            recorder?.release()
            recorder = null
            recordingThread = null
            newArray.clear()
        }

    }


    private fun writeAudioData(){
        val data = ByteArray(bufferSize / 2)
        while(isRecording){
            val read = recorder?.read(data, 0, data.size)
            Log.d("data", data.contentToString())
            Log.d("read", read.toString())
        }
    }

    //DATA COLLECTION - READ IN DATA INTO SHORT ARRAY
    private fun writeToVar(){
       // val baos = ByteArrayOutputStream()
        //val data = ByteArray(bufferSize/2)
        val shData = ShortArray(bufferSize/4)
       // val outputStream = FileOutputStream(file)
        while(isRecording){
            //val read = recorder?.read(data, 0, data.size)
            val readSh = recorder?.read(shData, 0, shData.size)
           // if (read != null) baos.write(data, 0, read)
            val outdata = shData.toMutableList()
            if (readSh!=null) {
                newArray.addAll(outdata)
                buffCount += shData.size
                if (buffCount == shData.size*8){
                    val f = get_dom_freq(newArray.toShortArray())
                    note_classifierTWO(f)
                    //Log.d("freq", f.toString())
                    newArray.clear()
                    buffCount=0
                }
                //val avg = shData.average()
           /*     Log.d("RECORD", ("${(((buffCount.toFloat() * 2) / samplingRate.toFloat())).toString()}, ${avg.toString()}"))
                //Log.d("time", (((buffCount.toFloat() * 2) / samplingRate.toFloat())).toString())
                //Log.d("avg", avg.toString())
                try {
                    outputStream.write(("${(((buffCount.toFloat() * 2) / samplingRate.toFloat())).toString()}, ${avg.toString()}\n").toByteArray())
                } catch (e: Exception) {
                    e.printStackTrace()
                }*/

            }
        }
        //audioByteArray = baos.toByteArray()
        val finalShortArray = newArray.toShortArray()
      //  outputStream.close()
      /*  val new = finalShortArray[0].toDouble()
        Log.d("finalShortArray[0] as short", finalShortArray[0].toString())
        Log.d("finalShortArray[0] as double", new.toString())
        val newTwo = finalShortArray[0] * 1.0
        Log.d("finalShortArray[0] *1.0", newTwo.toString())
        Log.d("newTwo type", newTwo::class.simpleName.toString())*/


    /*    Log.d("byte array", audioByteArray.contentToString())
        Log.d("byte array length", audioByteArray?.size.toString())
        Log.d("average", audioByteArray?.average().toString())
        Log.d("distinct", audioByteArray?.distinct().toString())
        Log.d("time?", (audioByteArray?.size!!.toFloat()/samplingRate.toFloat()).toString())

        Log.d("Short array", finalShortArray.contentToString())
        Log.d("Short array length", finalShortArray.size.toString())
        Log.d("average", finalShortArray.average().toString())
        Log.d("distinct", finalShortArray.distinct().toString())
        Log.d("time?", (((finalShortArray.size.toFloat()*2)/samplingRate.toFloat())).toString())*/

        val f = get_dom_freq(finalShortArray)
        note_classifier(f)
        //(f.toString() + "Hz").also { text.text = it }

    }


    //FEATURE EXTRACTION -- FFT THEN EXTRACT DOMINANT/HIGHEST AMP FREQ FOR NOTE CLASSIFICATION
    private fun get_dom_freq(short: ShortArray): Double{
       //FFT
        val signal = mutableListOf<Double>()
        short.mapTo(signal) {it * 1.0 }
        val output = signal.toDoubleArray()
        val ft: _Fourier = FastFourier(output)
        ft.transform()
        val onlyPositive = false
        val out = ft.getMagnitude(onlyPositive) //Full Absolute

        //get dom freq
        val freqs = get_fftFreq(out.size)
        val m = out.maxOrNull()
        if (m!=null){
            val freq = freqs[out.indexOfFirst { it == m }]
            val freq_hertz = abs(freq*samplingRate)
            Log.d("freq_hertz", freq_hertz.toString())
            return freq_hertz
        } else{
            return -1.0
        }


    }

    //based off of numpy's fft.fftfreq function
    //assist with DOM FREQ FEATURE EXTRACTION
    private fun get_fftFreq(size: Int): DoubleArray{
        val value = 1.0/size
        val output = mutableListOf<Double>()
        val N = (size-1)/2
        val p1 = (0..N).toList()
        val p2 = (-(size/2)..-1).toList()
        p1.mapTo(output) {
            it *value
        }
        p2.mapTo(output) {
            it*value
        }
        return output.toDoubleArray()
    }

    //classify notes --- need to clean up, binary decision tree?
    private fun note_classifier(freq: Double){
        if (freq>=370) {
            if (freq >= 465) {
                if (freq >= 480 && freq <= 540) {
                    text.text = "C"
                } else {
                    text.text = "B"
                }
            } else {
                if (freq>= 416){
                    text.text = "A"
                } else {
                    text.text = "G"
                }
            }
        } else {
            if (freq>=312){
                if(freq>=339){
                    text.text = "F"
                } else {
                    text.text = "E"
                }
            } else {
                if (freq>=250 && freq<276){
                    text.text= "C"
                } else {
                    text.text = "D"
                }
            }
        }
    }


    private fun note_classifierTWO(freq: Double){
        if (freq>=388.13){
            if (freq in 388.13..404.13){
                text.text="G"
            }
            else if (freq in 430.88..446.88){
                text.text="A"
            }
            else if (freq in 489.34..505.34){
                text.text="B"
            }
            else if(freq in 517.46..533.46){
                text.text="C"
            } else {
                text.text=""
            }

        }
        else{
            if (freq>=254.12){
                if (freq in 254.12..270.14){
                    text.text="C"
                }
                else if (freq in 290.14..306.14){
                    text.text="D"
                }
                else if (freq in 321.55..337.55){
                    text.text="E"
                }
                else if (freq in 344.99..364.00){
                    text.text="F"
                }
                else {
                    text.text=""
                }
            } else {
                text.text=""
            }

        }
    }
}