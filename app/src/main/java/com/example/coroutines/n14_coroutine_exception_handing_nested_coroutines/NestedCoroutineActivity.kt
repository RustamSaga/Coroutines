package com.example.coroutines.n14_coroutine_exception_handing_nested_coroutines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.coroutines.n11_coroutine_dispatcher.log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NestedCoroutineActivity: ComponentActivity() {
    private val tag = "myTag"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.wrapContentSize()){
                    Button(onClick = {
                       coroutineHandlerTest()
                    }) {
                        Text(text = "RUN")
                    }
                }

            }
        }
    }

    private fun coroutineHandlerTest() {
        val handler = CoroutineExceptionHandler { context, exception ->
            log(tag, "$exception was handled in Coroutine_${context[CoroutineName]?.name}")
        }

        val scope = CoroutineScope(Job() + Dispatchers.Default + handler)

        scope.launch(CoroutineName("1")) {

            launch(CoroutineName("1_1")) {
                TimeUnit.MILLISECONDS.sleep(500)
                Integer.parseInt("a")
            }

            launch(CoroutineName("1_2")) {
                TimeUnit.MILLISECONDS.sleep(1000)
            }
        }

        scope.launch(CoroutineName("2")) {

            launch(CoroutineName("2_1")) {
                TimeUnit.MILLISECONDS.sleep(1000)
            }

            launch(CoroutineName("2_2")) {
                TimeUnit.MILLISECONDS.sleep(1000)
            }
        }
    }

}
