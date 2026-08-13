package com.troxzy.trxchess.network

data class ApiError(val code:String,val message:String,val retryable:Boolean)
sealed interface ApiResult<out T>{data class Success<T>(val value:T):ApiResult<T>;data class Failure(val error:ApiError):ApiResult<Nothing>}
interface NetworkClient{suspend fun fetchRemoteConfig():ApiResult<Map<String,Boolean>>;suspend fun submitTelemetry(event:String,timestamp:Long,metrics:Map<String,Any?>):ApiResult<Unit>}
