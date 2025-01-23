package com.example.macrotracker
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.bumptech.glide.Glide
class Figma_861 : AppCompatActivity() {
	private var editTextValue1: String = ""
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_figma_861)
		Glide.with(this).load("https://storage.googleapis.com/tagjs-prod.appspot.com/v1/1b3RsUiuVw%2BPDzsdDr_AaC_50y0.png").into(findViewById(R.id.r9y460of4jzu))
		Glide.with(this).load("https://storage.googleapis.com/tagjs-prod.appspot.com/v1/1b3RsUiAADgZqfT6f8DKAU0CBnp.png").into(findViewById(R.id.rd9hc9tszfi))
		Glide.with(this).load("https://storage.googleapis.com/tagjs-prod.appspot.com/v1/1b3RsUiJyfpdLrDsfw_kNFo2gR4.png").into(findViewById(R.id.rn0do2in2wfk))
		Glide.with(this).load("https://storage.googleapis.com/tagjs-prod.appspot.com/v1/1b3RsUi96nO2Ww2Ho9_APhTlvhI.png").into(findViewById(R.id.rqsbkbanggq9))
		Glide.with(this).load("https://storage.googleapis.com/tagjs-prod.appspot.com/v1/1b3RsUiwzAMc3d39z8s612wth0A.png").into(findViewById(R.id.rodzt8k7vy0r))
		Glide.with(this).load("https://storage.googleapis.com/tagjs-prod.appspot.com/v1/1b3RsUimWDRa_LgY_wftqfr9Aik.png").into(findViewById(R.id.rkxd9xhe4zb))
		val editText1: EditText = findViewById(R.id.ryxljljb9ai)
		editText1.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
				// before Text Changed
			}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				editTextValue1 = s.toString()  // on Text Changed
			}
			override fun afterTextChanged(s: Editable?) {
				// after Text Changed
			}
		})
	}
}