package com.example.domain.common

import java.lang.Exception
import kotlin.coroutines.cancellation.CancellationException

sealed class Result<out T> {
    data class Success<out T>(val data : T) : Result<T>()
    data class Error(val throwable: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
    object Idle : Result<Nothing>()
}

inline fun <T> Result<T>.onSuccess(
    block :(T) -> Unit
) : Result<T>{
    return if(this is Result.Success){
        this.also { block(this.data) }
    }else this
}

inline fun <T> Result<T>.onError(
    block :(Throwable) -> Unit
) : Result<T>{
    return if(this is Result.Error){
        this.also { block(this.throwable) }
    }else this
}

inline fun <T> Result<T>.onLoading(
    block :() -> Unit
) : Result<T>{
    return if(this is Result.Loading){
        this.also { block() }
    }else this
}
//crossinline: chi cho phep return thoat ra khoi lambda,
suspend inline fun <T> tryCatch(
    crossinline block : suspend () -> T
) : Result<T>{
    return try{
        Result.Success(block())
    }catch (e : Exception){
        if(e is CancellationException) throw e//huy coroutine
        Result.Error(e)
    }
}

inline fun <T,A> Result<T>.map(
    trans :(T) -> A
) : Result<A>{
    return when(this){
        is Result.Success -> Result.Success(trans(this.data))
        is Result.Error -> Result.Error(this.throwable)
        is Result.Loading -> Result.Loading
        is Result.Idle -> Result.Idle
    }
}