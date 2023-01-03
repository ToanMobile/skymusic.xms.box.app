package com.toan_itc.core.richutils

import androidx.appcompat.app.AppCompatActivity

/**
 * Created by Toan.IT on 11/1/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

inline fun androidx.fragment.app.FragmentManager.inTransaction(func: androidx.fragment.app.FragmentTransaction.() -> androidx.fragment.app.FragmentTransaction) {
    beginTransaction().func().commitAllowingStateLoss()
}

fun AppCompatActivity.addFragment(fragment: androidx.fragment.app.Fragment, frameId: Int){
    supportFragmentManager.inTransaction { add(frameId, fragment) }
}

fun AppCompatActivity.replaceFragment(fragment: androidx.fragment.app.Fragment, frameId: Int) {
    supportFragmentManager.inTransaction{replace(frameId, fragment)}
}

fun androidx.fragment.app.FragmentActivity.removeFragment(fragment: androidx.fragment.app.Fragment) {
    supportFragmentManager.inTransaction { remove(fragment) }
}

fun androidx.fragment.app.FragmentActivity._switchFragment(from: androidx.fragment.app.Fragment?, to: androidx.fragment.app.Fragment, frameId: Int) {
    supportFragmentManager.inTransaction {
        if (from == null) {
            replace(frameId, to)
        } else {
            if (!to.isAdded) {
                hide(from).add(frameId, to)
            } else {
                hide(from).show(to)
            }
        }
    }
}

/**
 * The `fragment` is added to the container view with id `frameId`. The operation is
 * performed by the `fragmentManager`.
 */
fun AppCompatActivity.replaceFragmentInActivity(fragment: androidx.fragment.app.Fragment, frameId: Int) {
    supportFragmentManager.transact {
        replace(frameId, fragment)
    }
}

/**
 * The `fragment` is added to the container view with tag. The operation is
 * performed by the `fragmentManager`.
 */
fun AppCompatActivity.addFragmentToActivity(fragment: androidx.fragment.app.Fragment, tag: String) {
    supportFragmentManager.transact {
        add(fragment, tag)
    }
}
/**
 * Runs a FragmentTransaction, then calls commit().
 */
private inline fun androidx.fragment.app.FragmentManager.transact(action: androidx.fragment.app.FragmentTransaction.() -> Unit) {
    beginTransaction().apply {
        action()
    }.commit()
}