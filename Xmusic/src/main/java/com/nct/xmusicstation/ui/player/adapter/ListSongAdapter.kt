package com.nct.xmusicstation.ui.player.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nct.xmusicstation.R
import com.nct.xmusicstation.data.model.song.SongDetail
import com.nct.xmusicstation.utils.artists
import com.nct.xmusicstation.utils.getDurationToSecond
import com.nct.xmusicstation.utils.setSelecter
import com.wang.avi.AVLoadingIndicatorView

/**
 * Created by Toan.IT on 4/27/18.
 * Email: huynhvantoan.itc@gmail.com
 */

class ListSongAdapter : RecyclerView.Adapter<ListSongAdapter.ViewHolder>() {
    private var listData: MutableList<SongDetail>? = mutableListOf()
    private var indexSongPlay = 0
    private var indexSongOld = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_song_item, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            listData?.get(adapterPosition)?.apply {
                bind(indexSongPlay, indexSongOld, layoutPosition, this)
            }
        }
    }

    override fun getItemCount(): Int = listData?.size?:0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var txtPositionSong: TextView
        private lateinit var txtNameSong: TextView
        private lateinit var txtNameSinger: TextView
        private lateinit var txtDuration: TextView
        private lateinit var iconMusic: AVLoadingIndicatorView
        private lateinit var rootSong: ConstraintLayout

        @SuppressLint("SetTextI18n")
        fun bind(indexSongPlay: Int, indexSongOld: Int, position: Int, item: SongDetail) {
            with(itemView) {
                txtPositionSong = findViewById(R.id.txtPositionSong)
                txtNameSong = findViewById(R.id.txtNameSong)
                txtNameSinger = findViewById(R.id.txtNameSinger)
                txtDuration = findViewById(R.id.txtDuration)
                iconMusic = findViewById(R.id.iconMusic)
                rootSong = findViewById(R.id.rootSong)
                item.apply {
                    if(indexSongPlay == position){
                        //Logger.e("indexSongPlay == indexSongPlay == position$indexSongPlay")
                        txtPositionSong.isInvisible = true
                        iconMusic.isVisible = true
                        setSelecter(true, rootSong, txtPositionSong, txtNameSong, txtNameSinger, txtDuration)
                    }else{
                        //Logger.e("indexSongOld == positionindexSongOld == position$indexSongOld")
                        txtPositionSong.isVisible = true
                        iconMusic.isInvisible = true
                        setSelecter(false, rootSong, txtPositionSong, txtNameSong, txtNameSinger, txtDuration)
                    }
                    txtPositionSong.text = position.plus(1).toString()
                    txtNameSong.text = title
                    txtNameSinger.text = artists(artists)
                    txtDuration.text = getDurationToSecond(duration)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshData(listData: List<SongDetail>?) {
        listData?.let { newList ->
            //Logger.e("ListSongAdapter:refreshData="+newList.size)
            this.listData?.apply {
                clear()
                addAll(newList)
            }
            this.indexSongPlay = 0
            notifyDataSetChanged()
        }
    }

    fun isCheckListNull(): Boolean = itemCount == 0

    fun getList(): List<SongDetail>? = listData

    fun songPlay(position: Int){
        if(position == 0 && this.indexSongPlay == 0){
            //Logger.e("ListSongAdapter:==0")
            notifyItemChanged(this.indexSongPlay)
            this.indexSongOld = 0
        } else if(position != this.indexSongPlay) {
            this.indexSongOld = this.indexSongPlay
            this.indexSongPlay = position
            //Logger.e("ListSongAdapter:songPlay="+this.indexSongPlay +"indexSongOld="+indexSongOld+"position="+position)
            notifyItemChanged(this.indexSongOld)
            notifyItemChanged(this.indexSongPlay)
        }
    }

    fun songPlayFist(position: Int){
        if(position == 0){
            //Logger.e("ListSongAdapter:songPlayFist==0")
            notifyItemChanged(this.indexSongOld)
            this.indexSongOld = 0
        } else {
            this.indexSongPlay = position
            this.indexSongOld = this.indexSongPlay
            //Logger.e("ListSongAdapter:songPlay="+this.indexSongPlay +"indexSongOld="+indexSongOld+"position="+position)
            notifyItemChanged(this.indexSongOld)
            notifyItemChanged(this.indexSongPlay)
        }
    }
}