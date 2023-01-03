package com.nct.xmusicstation.binding

import androidx.databinding.DataBindingComponent
import androidx.fragment.app.Fragment

/**
 * Created by Toan.IT on 11/3/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

/**
 * A Data Binding Component implementation for fragments.
 */
class FragmentDataBindingComponent(fragment: Fragment) : DataBindingComponent {
    override fun getFragmentBindingAdapters(): FragmentBindingAdapters {
        return fragmentBindingAdapters
    }

    private val fragmentBindingAdapters: FragmentBindingAdapters = FragmentBindingAdapters(fragment)

}
