package com.devpro.pizzatime.feature.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegisterViewModel : ViewModel() {

    private val _registerResult = MutableLiveData<Result<Unit>>()
    val registerResult: LiveData<Result<Unit>> = _registerResult

    fun register(name: String, email: String, password: String) {
        FirebaseAuthRepository.register(
            name = name,
            email = email,
            password = password,
            onResult = { result -> _registerResult.postValue(result) },
        )
    }
}
