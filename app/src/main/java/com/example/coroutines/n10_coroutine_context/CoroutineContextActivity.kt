package com.example.coroutines.n10_coroutine_context

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.math.log


/**
 * CoroutineContext - это хранилище которое есть в scope. Хранит она в себе 2 основных элкмента: Job and Dispatcher
 * можно добавить свой элемент наследуясь от класса AbstractCoroutineContextElement
 *
 * Context чем-то похож на Map. Он хранит в себе элементы, и их можно достать по ключу.
 */
class CoroutineContextActivity : ComponentActivity() {
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            // create scope with context
            val scope = createScope(Job(), Dispatchers.Main)
            Log.d("myContext", "scope with fully context = $scope")

            // create scope. For scope don't need dispatcher
            val scopeWithJob = createScope(Job(), null)
            Log.d("myContext", "scope with job = $scopeWithJob")

            // create scope. The program itself creates "Job"
            val scopeWithDispatcher = CoroutineScope(Dispatchers.Main)
            Log.d("myContext", "scope with dispatcher = $scopeWithDispatcher")

            // create element for context
            val scopeWithMyElementContext = createElementContext()
            Log.d("myContext", "scope with myContext = $scopeWithMyElementContext")

            // job not transferred to daughter coroutine
            // but if there is no Dispatcher, the default Dispatcher will be used.
            scope.launch {
                Log.d("transfer", "parent scope = ${this.coroutineContext}")

                launch {
                    Log.d("transfer", "child scope = ${this.coroutineContext}")

                    launch(Dispatchers.Main) { // all children will be Dispatchers.Main
                        Log.d("transfer", "child scope = ${this.coroutineContext}")

                        launch {
                            Log.d("transfer", "child scope = ${this.coroutineContext}")
                        }
                    }
                }
            }
            // transfer our element of context to builder context
            val userData = UserData(2, "Malay", 32)
            scope.launch {
                Log.d("transferElement", "parent scope = ${this.coroutineContext}")

                launch(userData) {
                    Log.d("transferElement", "child scope = ${this.coroutineContext}")
                    val user = this.coroutineContext[UserData]
                    Log.d("transferElement", "user = $user")
                }
            }

        }
    }
}


fun createScope(job: Job?, dispatcher: CoroutineDispatcher?): CoroutineScope {

    return when {
        job != null && dispatcher != null -> {
            val context = Job() + Dispatchers.Default
            Log.d("myContext", "context = $context")
            return CoroutineScope(context)
        }

        job != null && dispatcher == null -> {
            return CoroutineScope(job)
        }

        job == null && dispatcher != null -> {
            return CoroutineScope(dispatcher)
        }

        else -> {
            CoroutineScope(Job())
        }
    }
}

fun createElementContext(): CoroutineScope {
    val userData = UserData(1, "BatIr", 30)
    return CoroutineScope(Job() + Dispatchers.Main + userData)
}

data class UserData(
    val id: Int,
    val name: String,
    val age: Int
) : AbstractCoroutineContextElement(UserData) {
    companion object Key : CoroutineContext.Key<UserData>
}