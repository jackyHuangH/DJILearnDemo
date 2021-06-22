package com.zenchn.djilearndemo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class PairMutableLiveData<A, B> : MutableLiveData<Pair<A, B>>() {

    fun setValue(first: A, second: B) {
        super.setValue(Pair(first, second))
    }

}

class PairObserver<A, B>(private val observer: (A, B) -> Unit) : Observer<Pair<A, B>> {
    override fun onChanged(t: Pair<A, B>) {
        observer.invoke(t.first, t.second)
    }
}

class TripleMutableLiveData<A, B, C> : MutableLiveData<Triple<A, B, C>>() {

    fun setValue(first: A, second: B, third: C) {
        super.setValue(Triple(first, second, third))
    }

}

class TripleObserver<A, B, C>(private val observer: (A, B, C) -> Unit) : Observer<Triple<A, B, C>> {
    override fun onChanged(t: Triple<A, B, C>) {
        observer.invoke(t.first, t.second, t.third)
    }
}

abstract class CacheEnableLiveData<T> : MutableLiveData<T>() {

    init {
        this.initValue()?.let { value = it }
    }

    abstract fun initValue(): T?

    abstract fun saveValue(value: T)

}