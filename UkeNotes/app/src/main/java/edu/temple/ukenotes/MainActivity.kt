package edu.temple.ukenotes

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.psambit9791.jdsp.filter.Butterworth
import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.signal.peaks.Peak
import com.github.psambit9791.jdsp.signal.peaks.Spike
import com.github.psambit9791.jdsp.transform.FastFourier
import com.github.psambit9791.jdsp.transform._Fourier
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
    val shortAudioDataArray = mutableListOf<Short>()
    val noteArray = mutableListOf<String>()
    lateinit var text: TextView
    lateinit var notesText: TextView
    var buffCount = 0


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

        text = findViewById(R.id.textView)
        notesText = findViewById(R.id.textView2)

        findViewById<Button>(R.id.recordButton).setOnClickListener {
            text.text = ""
            recorder = AudioRecord(audioSource, samplingRate, channelConfig, audioFormat, bufferSize)
            isRecording = true
            recorder?.startRecording()
            recordingThread = Thread({ writeAudioData() }, "AudioRecorder Thread")
            recordingThread?.start()
        }

       findViewById<Button>(R.id.stopButton).setOnClickListener {
           isRecording = false
           recorder?.stop()
           recorder?.release()
           recorder = null
           recordingThread = null
        }

    }


    //DATA COLLECTION - READ IN DATA INTO SHORT ARRAY
    private fun writeAudioData(){
        val tempArray = mutableListOf<Short>()
        val avgArray = mutableListOf<Double>()
        val shData = ShortArray(bufferSize/4)
        while(isRecording){
            val readSh = recorder?.read(shData, 0, shData.size)
            val outdata = shData.toMutableList()
            if (readSh!=null) {
                shortAudioDataArray.addAll(outdata)
                tempArray.addAll(outdata)
                buffCount += shData.size
                if (buffCount == shData.size*8){
                    //call processing/feature extraction
                    val f = get_fund_freq(tempArray.toShortArray())
                    note_classifier(f)
                    tempArray.clear()
                    buffCount=0
                }
            //collect average amp for .03 seconds, this is used to recognize when a note is strung
                //collecting average for more manageable # of data points for processing
                val avg = shData.average()
                avgArray.add(avg)
            }
        }
        //eval any remaining audio data here
        if (tempArray.size>0){
            //call processing/feature extraction
            val f = get_fund_freq(tempArray.toShortArray())
            note_classifier(f)
        }

        //check note strung events
        note_event_evaluation(avgArray.toDoubleArray())

    }


    //PROCESSING AND FEATURE EXTRACTION -- FFT THEN EXTRACT DOMINANT/HIGHEST AMP FREQ FOR NOTE CLASSIFICATION
    private fun get_fund_freq(short: ShortArray): Double{
       //get signal ready for functions by converting to double array
        val signal = mutableListOf<Double>()
        short.mapTo(signal) {it * 1.0 }
        val output = signal.toDoubleArray()

        //filter using bandpass filter to focus on ukulele notes frquency range
        val flt = Butterworth(output, samplingRate*1.0) //signal is of type double[]
        val result: DoubleArray =
            flt.bandPassFilter(3, 200.0, 800.0) //get the result after filtering

        //FFT
        val ft: _Fourier = FastFourier(result)
        ft.transform()
        val onlyPositive = false
        val out = ft.getMagnitude(onlyPositive)


        //get fundamental freq
        val freqs = get_fftFreq(out.size)
        val m = out.maxOrNull()
        if (m!=null){
            val freq = freqs[out.indexOfFirst { it == m }]
            val freq_hertz = abs(freq*samplingRate)
            return freq_hertz
        } else{
            return -1.0
        }


    }

    //based off of numpy's fft.fftfreq function
    //assist with FUND FREQ FEATURE EXTRACTION
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
        //when classifying, add to note array
        noteArray.add(text.text.toString())
    }


    //FURTHER PROCESSING AND FINAL FEATURE EXTRACTION
    private fun note_event_evaluation(data: DoubleArray){
        //use librabry to find spikes in original averaged signal
        val fp: FindPeak = FindPeak(data)
        val out: Spike = fp.spikes

        //filter spikes based on left side of peak
        val outLeftFilterAVG = out.filterByProperty(150.0, 900.0, "left")

        //check if spike is more than 10 apart -- if so add
        val outIndexArray = mutableListOf<Int>()
        outIndexArray.add(outLeftFilterAVG[0])
        for (i in 1 until outLeftFilterAVG.size){
            if (outLeftFilterAVG[i]-outLeftFilterAVG[i-1] >10){
                outIndexArray.add(outLeftFilterAVG[i])
            }
        }

        //use updated spikes array to find the related note
        val finalNoteArray = mutableListOf<String>()
        for (i in 0 until outIndexArray.size){
            val t = outIndexArray[i]*.03
            var index = kotlin.math.floor(t / .24).toInt()
            finalNoteArray.add(noteArray[index])
        }

        notesText.text = finalNoteArray.toString()
        noteArray.clear()
        shortAudioDataArray.clear()
    }
}