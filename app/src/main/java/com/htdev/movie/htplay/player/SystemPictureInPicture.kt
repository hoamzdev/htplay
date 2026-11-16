package com.htdev.movie.htplay.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.View
import androidx.media3.common.Player
import com.htdev.movie.htplay.util.SharePref

object SystemPictureInPicture {
    fun canEnter(activity: Activity, player: Player?): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            SharePref.init(activity).isEnablePip() && !activity.isFinishing && !activity.isDestroyed &&
            player != null && player.playWhenReady && player.playerError == null &&
            player.currentMediaItem != null &&
            (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING)

    fun update(activity: Activity, player: Player?, video: View?, enabled: Boolean = true): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return null
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
        val bounds = Rect()
        if (video?.getGlobalVisibleRect(bounds) == true) builder.setSourceRectHint(bounds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(enabled && canEnter(activity, player))
        }
        return builder.build().also { activity.setPictureInPictureParams(it) }
    }

    fun onUserLeaveHint(activity: Activity, player: Player?, video: View?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ||
            activity.isInPictureInPictureMode || !canEnter(activity, player)) return
        enter(activity, player, video)
    }

    fun enter(activity: Activity, player: Player?, video: View?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !canEnter(activity, player)) return false
        val params = update(activity, player, video) ?: return false
        // The OS can reject entry if PiP was disabled in system settings.
        return try {
            activity.enterPictureInPictureMode(params)
        } catch (_: IllegalStateException) {
            // Keep the normal background lifecycle when entry is unavailable.
            false
        }
    }
}
