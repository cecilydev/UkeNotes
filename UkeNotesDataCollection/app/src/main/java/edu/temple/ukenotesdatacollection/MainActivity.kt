package edu.temple.ukenotesdatacollection

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import androidx.core.view.get
import com.github.squti.androidwaverecorder.WaveRecorder
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    val storage = Firebase.storage
    var storageRef = storage.reference
    private var file: String? = null

    var currentNote: String? = null
    var currentMajor: String? = null
    var currentMinor: String? = null
    var current7: String? = null


    private var isRecording = false
    private var recorder: MediaRecorder? = null
    private var w_recorder: WaveRecorder? = null

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

        file = filesDir.absolutePath + "/audioFile.wav"
        w_recorder = WaveRecorder(file!!)
        w_recorder?.waveConfig?.sampleRate = 44100

        val recordNote = findViewById<Button>(R.id.buttonNote)
        val recordMajorNote = findViewById<Button>(R.id.buttonMajorChords)
        val recordMinorNote = findViewById<Button>(R.id.buttonMinorChord)
        val record7Note = findViewById<Button>(R.id.button7thChord)

        findViewById<RadioGroup>(R.id.radioGroupNotes).setOnCheckedChangeListener { radioGroup, i ->
            currentNote = radioGroup.findViewById<RadioButton>(i).text.toString()
        }

        recordNote.setOnClickListener {
            if (isRecording){
                recordNote.text ="Record"
                w_recorder?.stopRecording()
                val time = System.currentTimeMillis()
                val path = "Notes/" + currentNote + "/" +  time.toString() + ".wav"
                val newRef = storageRef.child(path)
                newRef.putFile(Uri.fromFile(File(file!!))).addOnCompleteListener{
                    Log.d("UPLOAD", "SUCCESS")
                   (File(file!!)).delete()
                }
                isRecording = false
            } else {
                recordNote.text ="Stop"
                w_recorder?.startRecording()
                isRecording = true
            }
        }


        findViewById<RadioGroup>(R.id.radioGroupMajorChords).setOnCheckedChangeListener { radioGroup, i ->
            currentMajor = radioGroup.findViewById<RadioButton>(i).text.toString()
        }

        recordMajorNote.setOnClickListener {
            if (isRecording){
                recordMajorNote.text ="Record"
                w_recorder?.stopRecording()
                val time = System.currentTimeMillis()
                val path = "Majors/" + currentMajor + "/" +  time.toString() + ".wav"
                val newRef = storageRef.child(path)
                newRef.putFile(Uri.fromFile(File(file!!))).addOnCompleteListener{
                    Log.d("UPLOAD", "SUCCESS")
                    (File(file!!)).delete()
                }
                isRecording = false
            } else {
                recordMajorNote.text ="Stop"
                w_recorder?.startRecording()
                isRecording = true
            }
        }


        findViewById<RadioGroup>(R.id.radioGroupMinorChords).setOnCheckedChangeListener { radioGroup, i ->
            currentMinor = radioGroup.findViewById<RadioButton>(i).text.toString()
        }

        recordMinorNote.setOnClickListener {
            if (isRecording){
                recordMinorNote.text ="Record"
                w_recorder?.stopRecording()
                val time = System.currentTimeMillis()
                val path = "Minors/" + currentMinor + "/" +  time.toString() + ".wav"
                val newRef = storageRef.child(path)
                newRef.putFile(Uri.fromFile(File(file!!))).addOnCompleteListener{
                    Log.d("UPLOAD", "SUCCESS")
                    (File(file!!)).delete()
                }
                isRecording = false
            } else {
                recordMinorNote.text ="Stop"
                w_recorder?.startRecording()
                isRecording = true
            }
        }


        findViewById<RadioGroup>(R.id.radioGroup2).setOnCheckedChangeListener { radioGroup, i ->
            current7 = radioGroup.findViewById<RadioButton>(i).text.toString()
        }

        record7Note.setOnClickListener {
            if (isRecording){
                record7Note.text ="Record"
                w_recorder?.stopRecording()
                val time = System.currentTimeMillis()
                val path = "7th/" + current7 + "/" +  time.toString() + ".wav"
                val newRef = storageRef.child(path)
                newRef.putFile(Uri.fromFile(File(file!!))).addOnCompleteListener{
                    Log.d("UPLOAD", "SUCCESS")
                    (File(file!!)).delete()
                }
                isRecording = false
            } else {
                record7Note.text ="Stop"
                w_recorder?.startRecording()
                isRecording = true
            }
        }


    }

}