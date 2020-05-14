package com.icecreamqaq.yuq.error

open class YuQException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
open class YuQRuntimeException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class ImageTypedException(message: String) : YuQRuntimeException(message = message)
