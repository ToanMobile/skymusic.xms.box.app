package com.nct.xmusicstation.data.remote

import com.orhanobut.logger.Logger
import retrofit2.Response
import java.io.IOException
import java.util.regex.Pattern

/**
 * Created by Toan.IT on 10/19/17.
 * Email:Huynhvantoan.itc@gmail.com
 */
/*
@SerializedName("3")
ACCESS_DENIED,
@SerializedName("102")
TOKEN_INVALID,
@SerializedName("104")
TOKEN_EXPIRED,
@SerializedName("110")
LOGIN_CODE_EXPIRED,
@SerializedName("111")
LOGIN_CODE_NOT_ASSIGNED,
@SerializedName("112")
LOGIN_CODE_INVALID,
@SerializedName("200")
USER_NOT_EXISTS,
@SerializedName("204")
USER_STOP_WORKING*/

class ApiResponse<T> {
    val code: Int
    val body: T?
    val errorMessage: String?
    val links: MutableMap<String, String>

    val isSuccessful: Boolean
        get() = code in 200..299

    val nextPage: Int?
        get() {
            val next = links[NEXT_LINK] ?: return null
            val matcher = PAGE_PATTERN.matcher(next)
            if (!matcher.find() || matcher.groupCount() != 1) {
                return null
            }
            try {
                return Integer.parseInt(matcher.group(1))
            } catch (ex: NumberFormatException) {
                Logger.w("cannot parse next page from %s", next)
                return null
            }

        }

    constructor(error: Throwable) {
        code = 500
        body = null
        errorMessage = error.message
        links = mutableMapOf()
    }

    constructor(response: Response<T>) {
        code = response.code()
        if (response.isSuccessful) {
            body = response.body()
            errorMessage = null
        } else {
            var message: String? = null
            if (response.errorBody() != null) {
                try {
                    message = response.errorBody()!!.string()
                } catch (ignored: IOException) {
                    Logger.d(ignored.message +"\n error while parsing response")
                }

            }
            if (message == null || message.trim { it <= ' ' }.isEmpty()) {
                message = response.message()
            }
            errorMessage = message
            body = null
        }
        val linkHeader = response.headers().get("link")
        if (linkHeader == null) {
            links = mutableMapOf()
        } else {
            links = androidx.collection.ArrayMap()
            val matcher = LINK_PATTERN.matcher(linkHeader)

            while (matcher.find()) {
                val count = matcher.groupCount()
                if (count == 2) {
                    links[matcher.group(2)] = matcher.group(1)
                }
            }
        }
    }

    companion object {
        private val LINK_PATTERN = Pattern
                .compile("<([^>]*)>[\\s]*;[\\s]*rel=\"([a-zA-Z0-9]+)\"")
        private val PAGE_PATTERN = Pattern.compile("\\bpage=(\\d+)")
        private val NEXT_LINK = "next"
    }

    override fun toString(): String {
        return "ApiResponse(code=$code, body=$body, errorMessage=$errorMessage, links=$links)"
    }
}
