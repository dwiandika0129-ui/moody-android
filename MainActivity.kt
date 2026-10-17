package com.moody.editor

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var trimStart: EditText
    private lateinit var trimEnd: EditText
    private lateinit var seek: SeekBar
    private lateinit var previewButton: Button
    private lateinit var exportButton: Button
    private var player: ExoPlayer? = null
    private var selectedVideo: Uri? = null
    private var durationMs: Long = 0L
    private var exportFile: File? = null
    private var transformer: Transformer? = null

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedVideo = uri
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            loadVideo(uri)
        }
    }
    private val saveExport = registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        val file = exportFile
        if (uri != null && file != null && file.exists()) {
            try {
                contentResolver.openOutputStream(uri)?.use { out -> FileInputStream(file).use { input -> input.copyTo(out) } }
                showStatus("Ekspor selesai. Video disimpan ke lokasi yang kamu pilih.")
            } catch (e: Exception) {
                showStatus("Gagal menyalin hasil: ${e.localizedMessage ?: "kesalahan file"}")
            }
        } else if (uri == null) {
            showStatus("Penyimpanan dibatalkan. Hasil sementara ada di cache aplikasi.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16,16,24)
        window.navigationBarColor = android.graphics.Color.rgb(16,16,24)
        buildUi()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float = 14f, color: Int = 0xFFEDEAF7.toInt()): TextView =
        TextView(this).apply { this.text = value; textSize = size; setTextColor(color) }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
            setBackgroundColor(0xFF101018.toInt())
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        header.addView(text("moody", 30f, 0xFFC3B2FF.toInt()))
        header.addView(text("VIDEO EDITOR  •  GRATIS", 11f, 0xFF9C98AD.toInt()))
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(space(12))
        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(0xFF242333.toInt())
        }
        root.addView(playerView, LinearLayout.LayoutParams(-1, dp(225)))
        root.addView(space(12))
        val pick = button("＋  Pilih video dari galeri") { pickVideo.launch(arrayOf("video/*")) }
        root.addView(pick, fullButtonParams())
        root.addView(space(14))
        root.addView(text("POTONG VIDEO", 12f, 0xFFC3B2FF.toInt()))
        root.addView(space(5))
        val times = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        trimStart = EditText(this).apply {
            hint = "Mulai (detik)"; setHintTextColor(0xFF89869B.toInt()); setTextColor(-1)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true); setPadding(dp(12)); setBackgroundColor(0xFF242333.toInt()); setText("0")
        }
        trimEnd = EditText(this).apply {
            hint = "Selesai (detik)"; setHintTextColor(0xFF89869B.toInt()); setTextColor(-1)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true); setPadding(dp(12)); setBackgroundColor(0xFF242333.toInt())
        }
        times.addView(trimStart, LinearLayout.LayoutParams(0, dp(52), 1f))
        val gap = View(this); times.addView(gap, LinearLayout.LayoutParams(dp(10), 1))
        times.addView(trimEnd, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(times)
        root.addView(space(8))
        seek = SeekBar(this).apply { max = 1000; progress = 0 }
        root.addView(seek, LinearLayout.LayoutParams(-1, -2))
        seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                if (user && durationMs > 0) player?.seekTo(durationMs * p / 1000)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        root.addView(space(5))
        previewButton = button("▶  Putar preview") {
            val p = player
            if (p == null) showStatus("Pilih video dulu.")
            else if (p.isPlaying) { p.pause(); previewButton.text = "▶  Putar preview" }
            else { p.play(); previewButton.text = "Ⅱ  Jeda preview" }
        }
        root.addView(previewButton, fullButtonParams())
        root.addView(space(10))
        exportButton = button("⇩  Ekspor potongan video") { exportTrimmedVideo() }
        root.addView(exportButton, fullButtonParams())
        root.addView(space(14))
        root.addView(text("FITUR BERIKUTNYA", 12f, 0xFFC3B2FF.toInt()))
        root.addView(space(5))
        root.addView(text("Teks, musik tambahan, filter warna, transisi, dan timeline multi-klip belum diimplementasikan di versi awal ini.", 13f, 0xFFAAA6BC.toInt()))
        root.addView(space(12))
        status = text("Pilih video untuk mulai. Hasil ekspor akan disimpan ke folder yang kamu pilih.", 12f, 0xFFBDB8D0.toInt())
        root.addView(status)
        setContentView(root)
    }

    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun fullButtonParams() = LinearLayout.LayoutParams(-1, dp(48))
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setTextColor(0xFFFFFFFF.toInt())
        backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF7055D9.toInt())
        setOnClickListener { action() }
    }
    private fun showStatus(s: String) { status.text = s }
    private fun loadVideo(uri: Uri) {
        try {
            player?.release()
            player = ExoPlayer.Builder(this).build().also { p ->
                playerView.player = p
                p.setMediaItem(MediaItem.fromUri(uri))
                p.prepare()
                p.addListener(object: Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            durationMs = p.duration.coerceAtLeast(0)
                            trimEnd.setText(String.format(Locale.US, "%.2f", durationMs / 1000.0))
                            showStatus("Video siap • durasi ${String.format(Locale.US, "%.1f", durationMs / 1000.0)} detik")
                        }
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        previewButton.text = if (isPlaying) "Ⅱ  Jeda preview" else "▶  Putar preview"
                    }
                })
            }
        } catch (e: Exception) {
            showStatus("Video tidak bisa dibuka: ${e.localizedMessage ?: "format tidak didukung"}")
        }
    }

    private fun exportTrimmedVideo() {
        val uri = selectedVideo
        if (uri == null) { showStatus("Pilih video terlebih dahulu."); return }
        val startMs = ((trimStart.text.toString().toDoubleOrNull() ?: 0.0) * 1000).toLong()
        val endMs = ((trimEnd.text.toString().toDoubleOrNull() ?: (durationMs / 1000.0)) * 1000).toLong()
        if (startMs < 0 || endMs <= startMs || (durationMs > 0 && endMs > durationMs)) {
            showStatus("Rentang waktu tidak valid. Pastikan mulai < selesai dan tidak melebihi durasi.")
            return
        }
        exportButton.isEnabled = false
        showStatus("Memproses video… tunggu sampai selesai, jangan tutup aplikasi.")
        try {
            val out = File(cacheDir, "moody_export_${System.currentTimeMillis()}.mp4")
            exportFile = out
            val clippedItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                ).build()
            val edited = EditedMediaItem.Builder(clippedItem).build()
            transformer = Transformer.Builder(this)
                .addListener(object: Transformer.Listener {
                    override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult) {
                        exportButton.isEnabled = true
                        showStatus("Potongan selesai dibuat. Pilih tempat untuk menyimpan video.")
                        saveExport.launch("moody_edit_${System.currentTimeMillis()}.mp4")
                    }
                    override fun onError(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult, exportException: androidx.media3.transformer.ExportException) {
                        exportButton.isEnabled = true
                        showStatus("Ekspor gagal: ${exportException.localizedMessage ?: "format video tidak didukung"}")
                    }
                }).build()
            transformer?.start(edited, out.absolutePath)
        } catch (e: Exception) {
            exportButton.isEnabled = true
            showStatus("Tidak bisa memulai ekspor: ${e.localizedMessage ?: "kesalahan"}")
        }
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        transformer?.cancel()
        super.onDestroy()
    }
}
