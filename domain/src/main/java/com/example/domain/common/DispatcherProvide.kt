package com.example.domain.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher

interface DispatcherProvide {
    val io : CoroutineDispatcher
    val main : MainCoroutineDispatcher
    val default : CoroutineDispatcher
}