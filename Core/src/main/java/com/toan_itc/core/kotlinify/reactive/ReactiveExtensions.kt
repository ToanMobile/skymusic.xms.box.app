package com.toan_itc.core.kotlinify.reactive

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by gilgoldzweig on 30/08/2017.
 * some small extensions function to allow easier usage with Reactive in android
 */
private val mainThread = AndroidSchedulers.mainThread()
private val newThread = Schedulers.newThread()
private val ioThread = Schedulers.io()

/**
 * observe on main thread
 * subscribe on new thread
 * unsubsidised on error and on complete and removes the need to handle it afterwards
 * @usage
 * someObservable
 *  .runSafeOnMain()
 *  .subscribe({}, {})
 */
fun <T> Observable<T>.runSafeOnMain(): Observable<T> =
    observeOn(mainThread)
        .subscribeOn(newThread)
        .doOnError {
            it.printStackTrace()
            unsubscribeOn(newThread)
            throw UnsupportedOperationException("onError exception")
        }
        .doOnComplete { unsubscribeOn(newThread) }


fun <T> Observable<T>.runSafeOnIO(): Observable<T> =
    observeOn(ioThread)
        .subscribeOn(newThread)
        .doOnError { unsubscribeOn(newThread) }
        .doOnComplete { unsubscribeOn(newThread) }

fun <T> Observable<T>.runSafeOnThread(): Observable<T> =
    observeOn(newThread)
        .subscribeOn(newThread)
        .doOnError {
            it.printStackTrace()
            unsubscribeOn(newThread)
            throw UnsupportedOperationException("onError exception")
        }
        .doOnComplete { unsubscribeOn(newThread) }

fun <T> Flowable<T>.runSafeOnMain(): Flowable<T> =
    observeOn(mainThread)
        .subscribeOn(newThread)
        .doOnError {
            it.printStackTrace()
            unsubscribeOn(newThread)
            throw UnsupportedOperationException("onError exception")
        }
        .doOnComplete { unsubscribeOn(newThread) }

fun <T> Flowable<T>.runSafeOnIO(): Flowable<T> =
    observeOn(ioThread)
        .subscribeOn(newThread)
        .doOnError {
            it.printStackTrace()
            unsubscribeOn(newThread)
            throw UnsupportedOperationException("onError exception")
        }
        .doOnComplete { unsubscribeOn(newThread) }

fun Completable.runSafeOnMain(): Completable =
    observeOn(mainThread)
        .subscribeOn(newThread)
        .doOnError { unsubscribeOn(newThread) }
        .doOnComplete { unsubscribeOn(newThread) }

fun Completable.runSafeOnIO(): Completable =
    observeOn(ioThread)
        .subscribeOn(newThread)
        .doOnError { unsubscribeOn(newThread) }
        .doOnComplete { unsubscribeOn(newThread) }


fun <T> Maybe<T>.runSafeOnMain(): Maybe<T> =
    observeOn(mainThread)
        .subscribeOn(newThread)
        .doOnError { unsubscribeOn(newThread) }
        .doOnSuccess { unsubscribeOn(newThread) }

fun <T> Maybe<T>.runSafeOnIO(): Maybe<T> =
    observeOn(ioThread)
        .subscribeOn(newThread)
        .doOnError({ unsubscribeOn(newThread) })
        .doOnSuccess { unsubscribeOn(newThread) }


fun <T> Single<T>.doIOapplyDatabase(): Single<T> {
    return this.subscribeOn(Schedulers.io())
        .observeOn(Schedulers.single())
}

fun <T> Flowable<T>.doIOapplyDatabase(): Flowable<T> {
    return this.subscribeOn(Schedulers.io())
        .observeOn(Schedulers.single())
}

fun <T> Flowable<T>.runSafeOnThread(): Flowable<T> =
    observeOn(newThread)
        .subscribeOn(newThread)
        .doOnError({
            it.printStackTrace()
            unsubscribeOn(newThread)
            throw UnsupportedOperationException("onError exception")
        })
        .doOnComplete { unsubscribeOn(newThread) }

fun <T> Flowable<T>.doNewThread(): Flowable<T> {
    return this.subscribeOn(Schedulers.newThread())
}

fun <T> Flowable<T>.doIO(): Flowable<T> {
    return this.subscribeOn(Schedulers.io())
}
