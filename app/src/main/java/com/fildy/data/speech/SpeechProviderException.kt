package com.fildy.data.speech

/** Any failure coming from a speech provider (network, auth, bad response…). */
class SpeechProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)
