package com.example.coroutines.n7_coroutine_scope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.logging.Logger

/**
 * Корутины, запущенные в этом scope, будут отменены, когда завершится ViewModel.
 * Аналогично есть scope и для Lifeсycle объектов.
 */
class MyViewModel: ViewModel() {
    init {
        viewModelScope.launch {
            // Coroutine that will be canceled when the ViewModel is cleared.
        }
    }


}