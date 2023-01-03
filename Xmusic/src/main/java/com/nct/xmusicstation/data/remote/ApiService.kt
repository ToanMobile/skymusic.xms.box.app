package com.nct.xmusicstation.data.remote

import com.nct.xmusicstation.data.model.auth.*
import com.nct.xmusicstation.data.model.song.AlbumInfo
import com.nct.xmusicstation.data.model.song.ListAlbum
import com.nct.xmusicstation.data.model.song.SongDetails
import io.reactivex.Flowable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Created by Toan.IT on 12/06/17.
 * Email:Huynhvantoan.itc@gmail.com
 * REST API access points
 */
interface ApiService {
    //Auth
    @FormUrlEncoded
    @POST("auth/token")
    fun getAccessToken(
            @Field("device_info") deviceInfo: String?,
            @Field("client_id") clientId: String,
            @Field("client_secret") clientSecret: String,
            @Field("grant_type") grantType: String): Flowable<TokenInfo>

    @FormUrlEncoded
    @POST("auth/token")
    fun refreshToken(
            @Field("device_info") deviceInfo: String,
            @Field("client_id") clientId: String,
            @Field("client_secret") clientSecret: String,
            @Field("grant_type") grantType: String,
            @Field("token") token: String,
            @Field("userId") userId: String): Flowable<TokenInfo>

    @FormUrlEncoded
    @POST("auth/login")
    fun login(
            @Field("device_info") deviceInfo: String,
            @Field("username") username: String,
            @Field("password") password: String): Flowable<LoginInfo>

    //Common
    @FormUrlEncoded
    @POST("common/update_version")
    fun getUpdateVersion(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String): Flowable<UpdateInfo>

    //XMS
    @FormUrlEncoded
    @POST("code/generate")
    fun getLoginCode(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("t") timestamp: Long,
            @Field("e") md5: String): Flowable<GenerateCodeInfo>

    @FormUrlEncoded
    @POST("code/retrieve")
    fun checkLogin(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String?,
            @Field("code") code: String?): Flowable<RetrieveModel>

    @FormUrlEncoded
    @POST("showcase/album")
    fun getShowcaseAlbums(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("page_index") pageIndex: Int,
            @Field("page_size") pageSize: Int): Flowable<NetworkResponse>

    @FormUrlEncoded
    @POST("album/newuser")
    fun getBrandAlbums(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("page_index") pageIndex: Int,
            @Field("page_size") pageSize: Int): Flowable<AlbumInfo>

    @FormUrlEncoded
    @POST("album/updateData")
    fun updateStatus(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("updated") updated: Int,
            @Field("setting") setting: String): Flowable<Status>

    @FormUrlEncoded
    @POST("album/detail")
    suspend fun getAlbumDetail(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("id") albumId: String): JsonObject<ListAlbum>

    @FormUrlEncoded
    @POST("xmusic/song_detail")
    fun getSongDetail(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("key") songKey: String): Flowable<SongDetails>

    @FormUrlEncoded
    @POST("user/profile")
    fun getUserProfile(@Field("device_info") deviceInfo: String,
                       @Field("token") token: String): Flowable<UserInfo>

    @FormUrlEncoded
    @POST("album/getPlayInfo")
    fun trackPlaySongStart(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("albumId") albumId: String,
            @Field("key") key: String,
            @Field("title") title: String,
            @Field("artists") artists: String): Flowable<Status>

    @FormUrlEncoded
    @POST("album/getDownloadInfo")
    fun trackDownloadSong(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("data") data: String): Flowable<Status>

    @FormUrlEncoded
    @POST("album/sendlog")
    fun sendLog(@Field("device_info") deviceInfo: String,
                @Field("token") token: String,
                @Field("jsonlog") data: String): Flowable<Status>

    @FormUrlEncoded
    @POST("tracking/play")
    fun trackPlaySong(
            @Field("device_info") deviceInfo: String,
            @Field("token") token: String,
            @Field("data") data: String): Flowable<Status>

}
