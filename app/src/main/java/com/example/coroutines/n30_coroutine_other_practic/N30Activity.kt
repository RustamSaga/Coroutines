package com.example.coroutines.n30_coroutine_other_practic

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class N30Activity : androidx.activity.ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("MissingInflatedId", "StateFlowValueCalledInComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = ViewModelProvider(this)[N30Model::class.java]

            val name = remember {
                mutableStateOf("")
            }
            val age = remember {
                mutableStateOf("")
            }
            val enabledButton = remember {
                mutableStateOf(false)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TextField(value = name.value, onValueChange = {
                    name.value = it
                    enabledButton.value = viewModel.validateFields(name.value, age.value)
                })
                TextField(
                    value = age.value,
                    onValueChange = {
                        age.value = it
                        enabledButton.value = viewModel.validateFields(name.value, age.value)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Button(enabled = enabledButton.value, onClick = {
                    Toast.makeText(this@N30Activity, "OK", Toast.LENGTH_LONG).show()
                }) {
                    Text(text = "GO")
                }
            }
        }
    }
}

class N30Model : ViewModel() {

    fun validateFields(name: String, age: String): Boolean =
        isAgeValid(age) && isNameValid(name)

    private fun isAgeValid(age: String): Boolean {
        return if (age.isNotBlank() && age.isDigitsOnly()) {
            age.toInt() >= 18
        } else false
    }

    private fun isNameValid(name: String): Boolean {
        return name.isNotBlank() && (name.first() == 'A')
    }



}