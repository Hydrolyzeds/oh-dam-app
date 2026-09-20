package org.myapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PuzzleActivity : AppCompatActivity() {

    private lateinit var puzzle: PuzzleView
    private lateinit var status: TextView
    private lateinit var shuffleButton: Button

    // System photo picker / file picker. No storage permission needed.
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) loadImage(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)

        puzzle = findViewById(R.id.puzzleView)
        status = findViewById(R.id.statusText)
        shuffleButton = findViewById(R.id.shuffleButton)

        puzzle.onProgress = { correct, total ->
            status.text = "$correct / $total pieces in place"
        }
        puzzle.onSolved = {
            status.text = "Solved!"
            Toast.makeText(this, "Puzzle complete!", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.pickButton).setOnClickListener {
            pickImage.launch("image/*")
        }
        shuffleButton.setOnClickListener {
            puzzle.shuffle()
        }
        shuffleButton.isEnabled = false
    }

    private fun loadImage(uri: Uri) {
        try {
            val bitmap = decodeScaled(uri, 1600)
            if (bitmap == null) {
                Toast.makeText(this, "Couldn't open that image", Toast.LENGTH_SHORT).show()
                return
            }
            puzzle.setImage(bitmap)
            shuffleButton.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open that image", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Decodes the image down to roughly [maxSide] pixels so big camera photos
     * don't run the phone out of memory.
     */
    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }
}

