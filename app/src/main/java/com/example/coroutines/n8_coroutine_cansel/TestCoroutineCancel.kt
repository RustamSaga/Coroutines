package com.example.coroutines.n8_coroutine_cansel

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
class TestCoroutineActivity : ComponentActivity() {

    private val scope1 = CoroutineScope(Job())
    private val scope2 = CoroutineScope(Job())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)) {
                    TestScopeDelay(scope = scope1)
                }
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)) {
                    TestScopeSleep(scope = scope2)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
        scope1.cancel()
    }
}

@Composable
fun TestScopeSleep(
    scope: CoroutineScope
) {
    lateinit var job: Job

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = {
            log("onRun, start")
            scope.launch {
                log("first coroutine, start")
                var x = 0
                while (x < 5 && isActive) {
                    TimeUnit.MILLISECONDS.sleep(1000)
                    log("first coroutine, ${x++}")
                }
                log("first coroutine, end")
            }
            log("onRun, middle")
            scope.launch {
                log("second coroutine, start")
                var x = 0
                while (x < 5 && isActive) {
                    TimeUnit.MILLISECONDS.sleep(1000)
                    log("second coroutine, ${x++}")
                }
                log("second coroutine, end")
            }

            job = scope.launch {
                log("third coroutine, start")
                var x = 0
                while (x < 5 && isActive) {
                    TimeUnit.MILLISECONDS.sleep(1000)
                    log("third coroutine, ${x++}")
                }
                log("third coroutine, end")
            }

            log("onRun, end")
        }) {
            Text(text = "RUN")
        }

        Button(onClick = {
            log("onCancel")
            scope.cancel() // for all coroutine
        } ) {
            Text(text = "CANCEL ALL")
        }

        Button(onClick = {
            log("onCancel")
            job.cancel() // for all coroutine
        }) {
            Text(text = "CANCEL ONE")
        }
    }

}

private fun log(text: String) {
    Log.d(
        "TestCoroutine",
        "${formatter.format(Date())} $text [${Thread.currentThread().name}]"
    )
}

/**
 * Когда внутри корутины используется suspend функция, поведение корутины при отмене может
 * отличаться от того, что мы рассмотрели выше. Попробуйте заменить
 * TimeUnit.MILLISECONDS.sleep(1000) на delay(1000). Эта suspend функция также делает паузу в
 * указанное количество миллисекунд.

При запуске и отмене корутины логи будут следующими:

        20:29:33.578 onRun, start [main]
        20:29:33.606 onRun, end [main]
        20:29:33.612 coroutine, start [DefaultDispatcher-worker-1]
        20:29:34.619 coroutine, 0, isActive = true [DefaultDispatcher-worker-1]
        20:29:35.622 coroutine, 1, isActive = true [DefaultDispatcher-worker-3]
        20:29:36.201 onCancel [main]

Отличие от предыдущих примеров в том, что функция delay при отмене корутины сразу прерывает
выполнение кода корутина. Метод log после delay не вызывается и до проверки isActive в цикле уже не доходит.

Мы еще поговорим, как это работает, когда будем изучать обработку ошибок в корутинах. Если вкратце,
то функция delay подписывается на событие отмены корутины, и при возникновении такого события
бросает специальный Exception. Именно поэтому выполнение кода корутины прерывается на delay и дальше
не идет. Но этот Exception ловится и обрабатывается внутренними механизмами корутины, поэтому
никакого крэша не происходит.

Такую специфику имеет не все suspend функции, а только cancellable. Они создаются аналогично тому,
как мы проходили в уроке про suspend функции, но вместо suspendCoroutine надо использовать suspendCancellableCoroutine.

 */

@Composable
fun TestScopeDelay(scope: CoroutineScope) {
    lateinit var job: Job

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = {
            log("onRun, start")
            scope.launch {
                log("first coroutine, start")
                var x = 0
                while (x < 5 && isActive) {
                    delay(1000)
                    log("first coroutine, ${x++}")
                }
                log("first coroutine, end")
            }
            log("onRun, middle")
            scope.launch {
                log("second coroutine, start")
                var x = 0
                while (x < 5 && isActive) {
                    delay(1000)
                    log("second coroutine, ${x++}")
                }
                log("second coroutine, end")
            }

            scope.launch {
                log("third coroutine, start")
                var x = 0
                while (x < 5 && isActive) {
                    delay(1000)
                    log("third coroutine, ${x++}")
                }
                log("third coroutine, end")
            }

            log("onRun, end")
        }) {
            Text(text = "RUN")
        }

        Button(onClick = {
            log("onCancel")
            scope.cancel() // for all coroutine
        }) {
            Text(text = "CANCEL ALL")
        }

        Button(onClick = {
            log("onCancel")
            job.cancel() // for one coroutine
        }) {
            Text(text = "CANCEL ONE")
        }
    }

}


