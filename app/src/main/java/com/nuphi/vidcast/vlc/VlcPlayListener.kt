package com.nuphi.vidcast.vlc

import android.view.View
import com.nuphi.vidcast.StreamListActivity
import com.nuphi.vidcast.task.XmlParserTask
//import android.support.v7.app.ActivityCompat.startActivityForResult
import android.content.Intent
import android.net.Uri
import android.content.ComponentName

class VlcPlayListener(val mainActivity: StreamListActivity, val sData: XmlParserTask.Companion.StreamData) : View.OnClickListener {
    private val vlcRequestCode = 42

    override fun onClick(v: View?) {
        val vlcIntent = Intent(Intent.ACTION_VIEW)
        vlcIntent.`package` = "org.videolan.vlc.debug"
        vlcIntent.setDataAndTypeAndNormalize(Uri.parse(sData.url.url), "video/*")
        vlcIntent.putExtra("title", sData.title)
        vlcIntent.putExtra("from_start", false)
        // vlcIntent.putExtra("position", 90000L)
        // vlcIntent.putExtra("subtitles_location", "/sdcard/Movies/Fifty-Fifty.srt")
        vlcIntent.component = ComponentName("org.videolan.vlc.debug", "org.videolan.vlc.gui.video.VideoPlayerActivity")
        mainActivity.startActivityForResult(vlcIntent, vlcRequestCode)
    }
}