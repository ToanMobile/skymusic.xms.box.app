package com.nct.xmusicstation.binding

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.nct.xmusicstation.R
import com.nct.xmusicstation.utils.GlideApp
import javax.inject.Inject

/**
 * Binding adapters that work with a fragment instance.
 */
class FragmentBindingAdapters
@Inject constructor(val fragment: Fragment) {

    @BindingAdapter("imageUrl")
    fun bindImage(imageView: ImageView, url: String?) {
        GlideApp.with(fragment)
                .load(url)
                .placeholder(R.drawable.ic_brand_avatar_default)
                .error(R.drawable.ic_brand_avatar_default)
                .into(imageView)
    }

    @BindingAdapter("imageDrawable")
    fun bindImage(imageView: ImageView, drawable: Int) {
        GlideApp.with(fragment)
                .load(drawable)
                .placeholder(R.drawable.ic_brand_avatar_default)
                .error(R.drawable.ic_brand_avatar_default)
                .into(imageView)
    }
}
