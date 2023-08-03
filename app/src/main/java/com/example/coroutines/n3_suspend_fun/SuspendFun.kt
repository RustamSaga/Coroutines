package com.example.coroutines.n3_suspend_fun

import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * suspend fun - Это просто маркер того, что данная функция умеет (и должна) работать с Continuation,
 *      чтобы приостановить выполнение корутины не блокируя поток. (маркер для одной операции switch)
 *
 * Чтобы suspend функция могла приостановить код не блокируя поток, ей нужен Continuation,
 *      выполнение которого она возобновит по завершению своей работы.
 *
 * Чтобы получить Continuation, используется функция suspendCoroutine
 *
 * Note: Мы не сможем запустить suspend функцию вне корутины, компилятор выдаст ошибку.
 * Потому что компилятор знает, что suspend функции нужен будет Continuation, который есть только в корутине
 * И вот именно для реализации этого ограничения и используется слово suspend.
 * Обычная функция, которая не является suspend, не сможет такого приостоновить код.
 * Она либо заблокирует поток корутины, либо попросит дать ей колбэк.
 *
 */
suspend fun download(url: String): File {
    val networkService = NetworkService()

    // suspendCoroutine - для старых легаси где колбеки
    return suspendCoroutine { continuation ->

        val file = getFileFromCache(url)

        if (file != null) {
            continuation.resume(file)
        } else {
            networkService.download(url, object : NetworkService.Callback {
                override fun onSuccess(result: File) {
                    continuation.resume(result)
                }

                override fun onFailure(error: Exception) {
                    continuation.resumeWithException(error)
                }

            })
        }
    }
}

fun getFileFromCache(url: String): File? {
    TODO("Not yet implemented")
}

class NetworkService {
    fun download(url: String, callback: Callback) {
        TODO("Not yet implemented")
    }

    interface Callback {
        fun onSuccess(result: File)
        fun onFailure(error: Exception)
    }

}
