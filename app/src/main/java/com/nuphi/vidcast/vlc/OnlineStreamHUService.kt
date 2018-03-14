package com.nuphi.vidcast.vlc

import android.net.Uri
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.view.View
import com.nuphi.vidcast.common.Types
import com.nuphi.vidcast.task.DownloadTask
import com.nuphi.vidcast.task.XmlParserTask
import org.videolan.vlc.extensions.api.VLCExtensionItem
import org.videolan.vlc.extensions.api.VLCExtensionItem.TYPE_VIDEO
import org.videolan.vlc.extensions.api.VLCExtensionService

/**
 * Created by gabre on 2/26/18.
 */
class OnlineStreamHUService: VLCExtensionService() {
    val listXml = "https://onlinestream.hu/list.xml"

    override fun browse(stringId: String?) {

    }

    public override fun onInitialize() {
        DownloadTask({ XmlParserTask({}, this::addItem, this::reportError).execute(it) }, this::reportError).execute(listXml)
    }

    override fun refresh() {
        // TODO implement this function.
    }

    private fun addItem(streamData: List<XmlParserTask.Companion.StreamData>) {
        val items = ArrayList<VLCExtensionItem>(streamData.size)
        streamData.map {
            val vlcItem = VLCExtensionItem()
                    .setLink(it.url.url)
                    .setTitle(it.title)
                    .setType(TYPE_VIDEO)
//        if (!TextUtils.isEmpty(dateDisplay)) {
//            vlcItem.setSubTitle(dateDisplay)
//        }
            if (it.picUrl.isNotEmpty()) {
                vlcItem.setImageUri(Uri.parse(it.picUrl))
            }
            vlcItem.getImageUri()
            items.add(vlcItem)
        }
        mServiceHandler.post(Runnable {
            updateList("Online Stream HU", items, false, false)
        })
    }

    private fun reportError(err: Types.VidCastError) {

    }
}