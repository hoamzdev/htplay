package com.example.data.exception


sealed class AppException(message : String) : Throwable(message){
    class EmailInvalidException : AppException( "No account found with this email")
    class EmailAlreadyExistsException  : AppException( "This email already exists")
    class PasswordWrongException : AppException("This password is incorrect")
    class NetworkException : AppException("Network error. Please check your connection")
    class UnknownException : AppException("Something went wrong. Please try again")
}