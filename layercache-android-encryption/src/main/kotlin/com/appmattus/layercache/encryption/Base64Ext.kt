package com.appmattus.layercache.encryption

import android.util.Base64

internal fun ByteArray.encodeBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
internal fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
