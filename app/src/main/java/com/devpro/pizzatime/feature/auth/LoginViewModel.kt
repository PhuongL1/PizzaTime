package com.devpro.pizzatime.feature.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    private val _loginResult = MutableLiveData<Result<AuthUserUiModel>>()
    val loginResult: LiveData<Result<AuthUserUiModel>> = _loginResult

    fun login(identifier: String, password: String) {
        _loginResult.value = FakeAuthRepository.login(
            identifier = identifier,
            password = password,
        )
    }
}