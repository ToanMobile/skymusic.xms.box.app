package com.nct.xmusicstation.ui.player.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nct.xmusicstation.R
import com.nct.xmusicstation.data.model.song.Schedule
import com.nct.xmusicstation.utils.setSelecter
import com.wang.avi.AVLoadingIndicatorView

/**
 * Created by Toan.IT on 4/27/18.
 * Email: huynhvantoan.itc@gmail.com
 */

class ScheduleAdapter : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {
    private var listData: MutableList<Schedule>? = mutableListOf()
    private var indexAlbumPlay = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_schedule_item, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            listData?.get(adapterPosition)?.apply {
                bind(indexAlbumPlay, adapterPosition, this)
            }
        }
    }

    override fun getItemCount(): Int = listData?.size ?: 0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var txtTimeSchedule: TextView
        private lateinit var txtNameSchedule: TextView
        private lateinit var rootSchedule: LinearLayout
        private lateinit var iconMusic: AVLoadingIndicatorView
        private lateinit var imgStar: AppCompatImageView

        @SuppressLint("SetTextI18n")
        fun bind(indexAlbumPlay: Int, position: Int, item: Schedule) {
            with(itemView) {
                txtTimeSchedule = findViewById(R.id.txtTimeSchedule)
                txtNameSchedule = findViewById(R.id.txtNameSchedule)
                rootSchedule = findViewById(R.id.rootSchedule)
                iconMusic = findViewById(R.id.iconMusic)
                imgStar = findViewById(R.id.img_star)
                item.apply {
                    if (indexAlbumPlay == position) {
                        //Logger.e("ScheduleAdapter:indexAlbumPlay == indexSongPlay == position$indexAlbumPlay" +"position="+position)
                        iconMusic.isVisible = true
                        setSelecter(true, rootSchedule, txtTimeSchedule, txtNameSchedule)
                    } else {
                        iconMusic.isInvisible = true
                        setSelecter(false, rootSchedule, txtTimeSchedule, txtNameSchedule)
                    }
                    if (item.ontop)
                        imgStar.isVisible = true
                    else
                        imgStar.isInvisible = true
                    txtTimeSchedule.text = "$fromTime-$toTime"
                    txtNameSchedule.text = albumName
                }
            }
        }
    }

    fun isCheckListNull(): Boolean = itemCount == 0

    @SuppressLint("NotifyDataSetChanged")
    fun refreshData(listData: List<Schedule>?, position: Int) {
        listData?.let { newData ->
            this.listData?.apply {
                clear()
                addAll(newData)
            }
            this.indexAlbumPlay = position
            notifyDataSetChanged()
        }
    }

    val getListData get() = listData
}