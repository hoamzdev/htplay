package com.example.data.common

import com.example.domain.common.DispatcherProvide
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher

data class DispatcherProvideImpl(
    override val default: CoroutineDispatcher,
    override val io: CoroutineDispatcher,
    override val main: MainCoroutineDispatcher
) : DispatcherProvide