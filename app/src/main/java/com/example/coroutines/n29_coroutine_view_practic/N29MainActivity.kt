package com.example.coroutines.n29_coroutine_view_practic

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.example.coroutines.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class N29MainActivity : AppCompatActivity() {

    lateinit var btn: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_n29_main)
        btn = findViewById(R.id.btn_id)

        lifecycleScope.launch {
            btn.text = "New text"
            Log.d("mainActivityTag", "old width is ${btn.width}")
            btn.awaitLayoutChange()
            Log.d("mainActivityTag", "new width is ${btn.width}")
        }

    }
}

suspend fun View.awaitLayoutChange() = suspendCancellableCoroutine { cont ->
    val listener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(view: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
            view?.removeOnLayoutChangeListener(this)
            cont.resume(Unit)
        }
    }
    addOnLayoutChangeListener(listener)
    cont.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
}