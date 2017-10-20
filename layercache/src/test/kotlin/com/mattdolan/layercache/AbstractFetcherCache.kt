package com.mattdolan.layercache

/* Workaround for "Cannot call abstract real method on java object"
see https://github.com/nhaarman/mockito-kotlin/issues/41 */
abstract class AbstractFetcherCache<Key: Any, Value: Any> : Fetcher<Key, Value>
