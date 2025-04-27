package com.arkdroid.mathspeech

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import io.github.derysudrajat.mathview.MathView
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.util.Log
import com.bumptech.glide.Glide
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import com.arkdroid.mathspeech.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


fun Bitmap.toUri(context: Context): Uri? {
    val file = File(context.cacheDir, "formula_${System.currentTimeMillis()}.png")
    try {
        FileOutputStream(file).use { stream ->
            this.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return FileProvider.getUriForFile(
                context,
                "com.arkdroid.mathspeech.provider", // Sesuaikan dengan authority
                file)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        val speechrecognize = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)

        val switchItemDebug = binding.navView.menu.findItem(R.id.switch_debug)
        val actionViewDebug = switchItemDebug.actionView
        val debugSwitch = actionViewDebug?.findViewById<SwitchCompat>(R.id.switches)

        val switchItemCopy = binding.navView.menu.findItem(R.id.switch_show_img)
        val actionViewCopy = switchItemCopy.actionView
        val copyImgSwitch = actionViewCopy?.findViewById<SwitchCompat>(R.id.switches)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_dehaze_24)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolBar,
            R.string.open_nav,
            R.string.close_nav
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.help -> showBottomMenu()
            }
            true
        }

        fun preprocessInput(input: String): String {
            return input
                .lowercase()
                .replace("nilai mutlak", "|")
                .replace("determinan", "det")
                .replace("ke-", " \\to{} ")
                .replace("lebih besar sama dengan", "\\geq{}")
                .replace("lebih kecil sama dengan", "\\leq{}")
                .replace("tidak sama dengan", "\\neq{}")
                .replace("lebih kecil", "<")
                .replace("lebih besar", ">")
                .replace("himpunan kosong", "\\varnothing{}")

                .replace("turunan kedua", "\"")
                .replace("turunan pertama", "'")

                .replace("binomial", "binoial")

                .replace("bukan anggota dari", "\\notin{}")
                .replace("anggota dari", "\\in{}")
                .replace("termuat dalam", "\\subset{}")
                .replace("sebanding dengan", "\\simeq{}")

                .replace("tambah", " + ")
                .replace("kurang", " - ")
                .replace("kali", " * ")
                .replace("bagi", " / ")
                .replace("per", " / ")
                .replace("pangkat", " ^ ")
                .replace("akar", " √ ")
                .replace("faktorial", " ! ")
                .replace("√", " √ ")
                .replace("(", " ( ") 
                .replace(")", " ) ")
                .replace("^", " ^ ") 
                .replace("*", " * ")
                .replace("/", " / ")
                .replace("+", " + ")
                .replace("-", " - ")
                .replace("=", " = ")
                .replace("sama dengan", " = ")
                .replace("atau", "\\lor{}")

                .replace("irisan", "\\cap{}")
                .replace("gabungan", "\\cup{}")
                .replace("mendekati", "\\approx{}")

                .replace("infinity", "\\infty{}")
                .replace("infinite", "\\infty{}")
                .replace("tak terhingga", "\\infty{}")
                .replace("tak hingga", "\\infty{}")

                .replace("menuju", "\\to{}")
                .replace("sampai", "\\to{}")
                .replace("hingga", "\\to{}")
                .replace("ke", "\\to{}")
                .replace("tegak lurus", "\\perp{}")

                .replace("bilangan phi", "\\pi{}")
                .replace("bilangan p", "\\pi{}")
                .replace("alfa", "\\alpha{}")
                .replace("beta", "\\beta{}")
                .replace("gamma", "\\gamma{}")
                .replace("delta", "\\Delta{}")
                .replace("teh tah", "\\theta{}")
                .replace("tehta", "\\theta{}")
                .replace("teta", "\\theta{}")
                .replace("lamda", "\\lambda{}")
                .replace("roh", "\\rho{}")
                .replace("omega", "\\omega{}")
                .replace("rasio emas", "\\phi{}")
                .replace("kaikai", "\\chi{}")
                .replace("derajat", "\\degree{}")
                .replace("°", "\\degree{}")
                .replace("x bar", "xbar")
                .replace("blog", "log")
                .replace("love", "log")
                .replace("lock", "log")
                .replace("log natural", "ln")
                .replace("second", "sekan")
                .replace("adjoin", "adj")

                .replace("satu", " 1 ")
                .replace("dua", " 2 ")
                .replace("tiga", " 3 ")
                .replace("empat", " 4 ")
                .replace("lima", " 5 ")
                .replace("enam", " 6 ")
                .replace("tujuh", " 7 ")
                .replace("delapan", " 8 ")
                .replace("sembilan", " 9 ")
                .replace("nol", " 0 ")
                .replace("om", "\\Omega{}")
                .replace("dek", "d")
                .replace("deh", "d")
                .replace("eular", "e")
                .replace("euler", "e")
                .replace("iular", "e")
                .replace("ular", "e")
                .replace("minus", " - ")
                .replace("min", " - ")
                .replace("sigma", "\\sigma")


                .replace("\\s+".toRegex(), " ")
                .trim()
        }

        fun parseMathToLatex(input: String): String {
            val latexBuilder = StringBuilder()
            val tokens = input.split(" ")
            var i = 0

            while (i < tokens.size) {
                when (tokens[i]) {
                    "integral" -> {
                        latexBuilder.append("\\int")

                        if (i + 1 < tokens.size && tokens[i + 1] == "dari") {
                            i++

                            if (i + 1 < tokens.size) {
                                latexBuilder.append("_{${tokens[i + 1]}}")
                                i++

                                if (i + 1 < tokens.size && tokens[i + 1] == "\\to{}") {
                                    i++
                                    if (i + 1 < tokens.size) {
                                        latexBuilder.append("^{${tokens[i + 1]}}")
                                        i++
                                    }
                                }
                            }
                        } else {
                            latexBuilder.append("{}")
                        }
                    }

                    "√" -> { // Konversi "√" ke "\sqrt"
                        latexBuilder.append("\\sqrt")

                        if (i + 1 < tokens.size && tokens[i + 1] == "\\to{}") {
                            i++
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("[${tokens[i + 1]}]")
                                i++

                                if (i + 1 < tokens.size && tokens[i + 1] ==  "dari") {
                                    i++
                                    latexBuilder.append("{")
                                    while (i + 1 < tokens.size && tokens[i + 1] !in listOf(
                                            "/", "=", "tutup"
                                    )) {
                                        latexBuilder.append("${tokens[i + 1]}")
                                        i++
                                    }
                                    latexBuilder.append("}")
                                }
                            }
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("{${tokens[i + 1]}}")
                                i++
                            }
                        }
                    }

                    "^" -> { 
                        latexBuilder.append("^")

                        if (i + 1 < tokens.size) {
                            latexBuilder.append("{${tokens[i + 1]}")
                            i++

                            if (i + 1 < tokens.size && tokens[i + 1] == "^") {
                                latexBuilder.append("^")
                                i++
                                if (i + 1 < tokens.size) {
                                    latexBuilder.append(tokens[i + 1])
                                    i++
                                }
                            }

                            latexBuilder.append("}")
                        }
                    }

                    "limit" -> {
                        latexBuilder.append("\\lim")

                        if (i + 1 < tokens.size) {
                            latexBuilder.append("_{${tokens[i + 1]}")
                            i++
                            if (i + 1 < tokens.size && tokens[i + 1] == "\\to{}")  {
                                latexBuilder.append("\\to{}")
                                i++
                                if (i + 1 < tokens.size) {
                                    latexBuilder.append("${tokens[i + 1]}")
                                    i++
                                }
                            }
                            latexBuilder.append("}")
                        }
                    }

                    "binoial" -> {
                        latexBuilder.append("\\binom")
                        if (i + 1 < tokens.size) {
                            latexBuilder.append("{${tokens[i + 1]}}")
                            i++
                            if (i + 1 < tokens.size && tokens[i + 1] == "dan") {
                                i++
                                if (i + 1 < tokens.size) {
                                    latexBuilder.append("{${tokens[i + 1]}}")
                                    i++
                                }
                            }
                        }
                    }

                    "mutasi" -> {
                        latexBuilder.append("P(")

                        if (i + 1 < tokens.size) {
                            latexBuilder.append("${tokens[i + 1]},")
                            i++
                            if (i + 1 < tokens.size && tokens[i + 1] == "ambil") {
                                i++
                                if (i + 1 < tokens.size) {
                                    latexBuilder.append(tokens[i + 1])
                                    i++
                                }
                            }
                        }

                        latexBuilder.append(")")
                    }

                    "jumlah" -> {
                        latexBuilder.append("\\sum")
                        if (i + 1 < tokens.size && tokens[i + 1] == "dari") {
                            i++

                            if (i + 1 < tokens.size) {
                                latexBuilder.append("_{${tokens[i + 1]}")
                                i++
                                if (i + 1 < tokens.size && tokens[i + 1] == "=") {
                                    latexBuilder.append("${tokens[i + 1]}")
                                    i++
                                    if (i + 1 < tokens.size) {
                                        latexBuilder.append("${tokens[i + 1]}")
                                        i++
                                    }
                                }
                                latexBuilder.append("}")
                            }

                            if (i + 1 < tokens.size && tokens[i + 1] == "\\to{}") {
                                i++
                                if (i + 1 < tokens.size) {
                                    latexBuilder.append("^{${tokens[i + 1]}}")
                                    i++
                                }
                            }
                        }
                    }

                    "/" -> {
                        if (i - 1 >= 0 && i + 1 < tokens.size) {
                            val numerator = tokens[i - 1]
                            val denominator = tokens[i + 1]

                            latexBuilder.setLength(latexBuilder.length - numerator.length)

                            latexBuilder.append("\\frac{$numerator}{$denominator}")

                            i += 1 // Loop utama akan menambah `i` lagi
                        }
                    }

                    "fungsi" -> {
                        if (i + 1 < tokens.size) {
                            latexBuilder.append(tokens[i + 1])
                            i++

                            if (i + 1 < tokens.size && (tokens[i + 1] == "'" || tokens[i + 1] == "\"")) {
                                latexBuilder.append(tokens[i + 1])
                                i++
                            }

                            if (i + 1 < tokens.size && tokens[i + 1] == "dari") {
                                i++
                                if (i + 1 < tokens.size) {
                                    latexBuilder.append("(${tokens[i + 1]})")
                                    i++
                                }
                            }
                        }
                    }

                    "log" -> {
                        latexBuilder.append("\\log")

                        if (i + 1 < tokens.size && tokens[i + 1] == "basis") {
                            i++
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("_${tokens[i + 1]}")
                                i++

                                if (i + 1 < tokens.size && tokens[i + 1] == "dari") {
                                    i++
                                    if (i + 1 < tokens.size) {
                                        latexBuilder.append("(${tokens[i + 1]})")
                                        i++
                                    }
                                }
                            }
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("(${tokens[i + 1]})")
                                i++
                            } else {
                                latexBuilder.append("{}")
                            }
                        }


                    }

                    "ln" -> {
                        latexBuilder.append("\\ln")

                        if (i + 1 < tokens.size && tokens[i + 1] == "basis") {
                            i++
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("_${tokens[i + 1]}")
                                i++

                                if (i + 1 < tokens.size && tokens[i + 1] == "dari") {
                                    i++
                                    if (i + 1 < tokens.size) {
                                        latexBuilder.append("(${tokens[i + 1]})")
                                        i++
                                    }
                                }
                            }
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("(${tokens[i + 1]})")
                                i++
                            } else {
                                latexBuilder.append("{}")
                            }
                        }
                    }

                    "sin" -> {
                        latexBuilder.append("\\sin")

                        if (i + 1 < tokens.size && tokens[i + 1] == "^") {
                            i++
                            latexBuilder.append("^{${tokens[i + 1]}}")
                            i++
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("(${tokens[i + 1]})")
                                i++
                            } else {
                                latexBuilder.append("{}")
                            }
                        }

                    }

                    "cos" -> {
                        latexBuilder.append("\\cos")

                        if (i + 1 < tokens.size && tokens[i + 1] == "^") {
                            i++
                            latexBuilder.append("^{${tokens[i + 1]}}")
                            i++
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("(${tokens[i + 1]})")
                                i++
                            } else {
                                latexBuilder.append("{}")
                            }
                        }

                    }

                    "tan" -> {
                        latexBuilder.append("\\tan")

                        if (i + 1 < tokens.size && tokens[i + 1] == "^") {
                            i++
                            latexBuilder.append("^{${tokens[i + 1]}}")
                            i++
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("(${tokens[i + 1]})")
                                i++
                            } else {
                                latexBuilder.append("{}")
                            }
                        }

                    }

                    "kosek" -> {
                        latexBuilder.append("\\csc")

                        if (i + 1 < tokens.size && tokens[i + 1] == "^") {
                            i++
                            latexBuilder.append("^{${tokens[i + 1]}}")
                            i++
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("(${tokens[i + 1]})")
                                i++
                            } else {
                                latexBuilder.append("{}")
                            }
                        }

                    }

                    "sekan" -> {
                        latexBuilder.append("\\sec")

                        if (i + 1 < tokens.size && tokens[i + 1] == "^") {
                            i++
                            latexBuilder.append("^{${tokens[i + 1]}}")
                            i++
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("(${tokens[i + 1]})")
                                i++
                            } else {
                                latexBuilder.append("{}")
                            }
                        }

                    }

                    "kotangen" -> {
                        latexBuilder.append("\\cot")

                        if (i + 1 < tokens.size && tokens[i + 1] == "^") {
                            i++
                            latexBuilder.append("^{${tokens[i + 1]}}")
                            i++
                        } else {
                            if (i + 1 < tokens.size) {
                                latexBuilder.append("(${tokens[i + 1]})")
                                i++
                            } else {
                                latexBuilder.append("{}")
                            }
                        }

                    }

                    "xbar" -> {
                        latexBuilder.append("\\overline")

                        if (i + 1 < tokens.size) {
                            latexBuilder.append("{${tokens[i + 1]}}")
                            i++
                        }
                    }

                    "matriks" -> {
                        if (i + 1 < tokens.size) {
                            latexBuilder.append("\\begin{pmatrix}")
                            i++
                        }

                        while (i + 1 < tokens.size) {
                            when (tokens[i]) {
                                "baris" -> {
                                    latexBuilder.append("\\\\\n")
                                    i++
                                }
                                "dan" -> {
                                    latexBuilder.append("&")
                                    i++
                                }
                                else -> {
                                    latexBuilder.append(tokens[i++])
                                }
                            }
                        }

                        latexBuilder.append("\\end{pmatrix}")
                    }


                    "*" -> latexBuilder.append("\\times{}") 
                    "-" -> latexBuilder.append("-")      
                    "+" -> latexBuilder.append("+")      

                    else -> latexBuilder.append(tokens[i]) 
                }
                i++
            }

            return latexBuilder.toString()
        }

        with(binding) {
            speechrecognize.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p0: Bundle?) {
                    Log.d("ready", "onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("begin", "onBeginningOfSpeech")
                }

                @SuppressLint("SetTextI18n")
                override fun onResults(p0: Bundle?) {
                    val matches = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                    if (!matches.isNullOrEmpty()) {
                        val processing = preprocessInput(matches[0])
                        if (binding.switchconnect.isChecked) {
                            val combined = "${edtInputMath.text} $processing"
                            val combinedLatex = parseMathToLatex(combined)
                            edtInputMath.setText(combinedLatex)
                        } else {
                            val latex = parseMathToLatex(processing)
                            edtInputMath.setText(latex)

                        }

                        rawData.text = matches[0]
                        preprocessData.text = processing
                    }
                }

                override fun onError(p0: Int) {
                    Log.e("SpeeechRecognizer", "Error: $p0")
                }

                override fun onEndOfSpeech() {
                    speechrecognize.stopListening()
                    speechrecognize.cancel()
                    Log.d("ending", "onEndOfSpeech")
                }

                override fun onPartialResults(p0: Bundle?) {
                    val matches = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d("SpeechRecognizer", "Partial results: ${matches?.get(0)}")
                    Log.d("partialing", "onPartialResults")
                }

                override fun onEvent(p0: Int, p1: Bundle?) {
                    Log.d("event", "onEvent")
                }

                override fun onBufferReceived(p0: ByteArray?) {
                    Log.d("buffs", "onBufferReceived")
                }

                override fun onRmsChanged(p0: Float) {
                    Log.d("rms", "onRmsChanged")
                }
            })

            val rumusAwal =
                "\\lim_{x\\to\\infty}\\binom{x}{y}\\sum_{n=0}^{\\infty}\\frac{1^{2}\\to\\infty}{\\sqrt{3c^{2}\\times2}}"
            mathView.apply {
                setTextAlignment(MathView.TextAlignment.CENTER)
                setTextColor(R.color.white)
                mathView.setTextColor("#76081C")
                mathView.setTextColor(MathView.RGB(118, 8, 28))
                mathView.setTextColor(MathView.RGB.WHITE)
                formula = rumusAwal
            }
            edtInputMath.setText(rumusAwal)
            btnRender.setOnClickListener {
                val rumus = edtInputMath.text.toString()
                mathView.setMathViewEngine(MathView.MathViewEngine.MATH_JAX)
                if (rumus.isNotBlank()) mathView.formula = rumus
            }
            btnCopy.setOnClickListener {
                val latexUrl = URLEncoder.encode(edtInputMath.text.toString(), "UTF-8")
                val imgUrl = "https://latex.codecogs.com/png.latex?\\dpi{200} \\large $latexUrl"
                Glide.with(this@MainActivity).load(imgUrl).into(imageView)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val url = URL(imgUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.doInput = true
                        connection.connect()

                        val inputStream = connection.inputStream
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        withContext(Dispatchers.Main) {
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val uri = bitmap.toUri(this@MainActivity)
                            val clipData = ClipData.newUri(contentResolver, "Latex Formula", uri)
                            clipboard.setPrimaryClip(clipData)
                            Toast.makeText(
                                this@MainActivity,
                                "Gambar tersalin!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            btnRec.setOnClickListener {
                speechrecognize.startListening(intent)
            }

            debugSwitch?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    rawData.visibility = View.VISIBLE
                    preprocessData.visibility = View.VISIBLE
                } else {
                    rawData.visibility = View.GONE
                    preprocessData.visibility = View.GONE
                }
            }

            copyImgSwitch?.setOnCheckedChangeListener {_, isChecked ->
                imageView.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showBottomMenu() {
        val dialog = AppCompatDialog(this)
        dialog.requestWindowFeature(WindowCompat.FEATURE_ACTION_BAR)
        dialog.setContentView(R.layout.bottom_menu)

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes!!.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
}