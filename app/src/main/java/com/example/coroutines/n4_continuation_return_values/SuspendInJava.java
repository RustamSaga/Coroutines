package com.example.coroutines.n4_continuation_return_values;

import android.util.Log;
import com.example.coroutines.n3_suspend_fun.NetworkService;
import java.io.File;
import java.util.logging.Logger;

import kotlin.coroutines.Continuation;


//  async{
//        val url = buildUrl()
//        val file = download(url) // suspend function
//        toast("File is downloaded: $url")
//        val size = unzip(file) // suspend function
//        toast("File is unzipped, size = $size")
//        size
//  }

class SafeContinuation {
    private final Object COROUTINE_SUSPENDED = new SafeContinuation.CoroutineSuspended();
    int label;
    String url;
    File file;
    Long size;

    public SafeContinuation(Continuation continuation) {
        // code
    }

    NetworkService networkService;

    Object download(String url, Continuation continuation) {

        // SafeContinuation - обертка для нашего Continuation.
        // Этот класс нужен для двухрежимной логики - передать результат который запрашивался
        // в билдере либо передать COROUTINE_SUSPENDED - который говорит что корутина еще не закончила выполнение
        SafeContinuation safeContinuation = new SafeContinuation(continuation);

        File file = getFileFromCache(url);

        if (file != null) {
            safeContinuation.resume(file);
        } else {
            networkService.download(url, new NetworkService.Callback() {
                @Override
                public void onSuccess(File result) {
                    safeContinuation.resume(result);
                }

                @Override
                public void onFailure(Exception error) {
                    safeContinuation.resumeWithException(error);
                }
            });
        }

        // getOrThrow() вернет либо запрашиваемый файл, либо COROUTINE_SUSPENDED, либо выбросит исключение
        // вернет файл в двух случая
        //      - если файл уже был, его не надо загружать и соответственно запускать корутину нет нужды.
        //      - когда корутина запустилась что бы загрузить файл, при завершении загрузки возвращает файл
        // вернет COROUTINE_SUSPENDED - когда в теле билдера прописана susoend fun которая приостанавливает
        //      код (в данном случае для загрузки файла)
        // вернет исключение (ошибку) - если вдруг связь оборвалась или нет интернета.
        return safeContinuation.getOrThrow();
    }

    private File getFileFromCache(String url) {
        return null;
    }

    void resume(Object result) {
        Object outcome = invokeSuspend(result);
        if (outcome == COROUTINE_SUSPENDED) return;
        completeCoroutine(outcome);
    }

    private void completeCoroutine(Object outcome) {
        // code
    }

    public void resumeWithException(Exception error) {
        // code
    }

    public Object getOrThrow() {
        return null;
    }

    Object invokeSuspend(Object result) {
        switch (label) {
            case 0: {
                url = buildUrl();
                label = 1;

                result = download(url, this);
                if (result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED;
            }
            case 1: {
                file = (File) result;
                Log.d("File", "Url is downloaded: " + url);
                label = 2;

                result = unzip(file, this);
                if (result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED;
            }
            case 2: {
                size = (Long) result;
                Log.d("File", "File is unzipped, size: " + size);
                return size;
            }
        }

        throw new IllegalStateException("...");
    }

    private Object unzip(File file, SafeContinuation suspendInJava) {
        return null;
    }

    private Object download(String url, SafeContinuation suspendInJava) {
        return null;
    }

    private String buildUrl() {
        return null;
    }


    static class CoroutineSuspended {
    }


    public Logger LOGGER = Logger.getLogger("Logger");
}