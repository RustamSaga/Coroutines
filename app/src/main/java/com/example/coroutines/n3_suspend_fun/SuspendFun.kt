package com.example.coroutines.n3_suspend_fun

import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine



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
