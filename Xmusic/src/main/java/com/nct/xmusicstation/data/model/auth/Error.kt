package com.nct.xmusicstation.data.model.auth

data class Error(
        val msg: String? = null,
        val code: Int? = null
)

data class Status(
        var status: String //ok
)