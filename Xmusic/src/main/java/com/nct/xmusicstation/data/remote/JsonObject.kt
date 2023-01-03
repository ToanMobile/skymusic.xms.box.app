package com.nct.xmusicstation.data.remote

import com.google.gson.annotations.Expose

/**
 * Created by ToanDev on 28/2/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

class JsonObject<T> {
    @Expose
    var data: T? = null
}

