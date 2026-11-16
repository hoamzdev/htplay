package com.htdev.movie.htplay.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharePref {
    private lateinit var sharePreference : SharedPreferences
    companion object{
        fun init(context: Context) : SharePref{
            val sharePref = SharePref()
            sharePref.sharePreference = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            return sharePref
        }
    }

    fun countOpenApp(){
        val currentOpenCount = sharePreference.getInt("open_count", 0)
        sharePreference.edit {
            putInt("open_count", currentOpenCount + 1)
        }
    }

    fun getOpenCountApp() : Int = sharePreference.getInt("open_count", 0)


    fun setAutoNextEpisode(ok : Boolean){
        sharePreference.edit {
            putBoolean("auto_next_episode", ok)
        }
    }

    fun isNextEpisode() : Boolean = sharePreference.getBoolean("auto_next_episode", false)


    fun enableNotification(ok : Boolean){
        sharePreference.edit{
            putBoolean("notification", ok)
        }
    }

    fun isEnableNotification() = sharePreference.getBoolean("notification", false)


    fun enablePictureInPicture(ok : Boolean){
        sharePreference.edit {
            putBoolean("pip", ok)
        }
    }

    fun isEnablePip() = sharePreference.getBoolean("pip", false)

}