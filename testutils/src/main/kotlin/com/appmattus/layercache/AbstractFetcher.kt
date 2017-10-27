package com.appmattus.layercache

/* Workaround for "Cannot call abstract real method on java object"
see https://github.com/nhaarman/mockito-kotlin/issues/41 */
abstract class AbstractFetcher<Key : Any, Value : Any> : Fetcher<Key, Value>