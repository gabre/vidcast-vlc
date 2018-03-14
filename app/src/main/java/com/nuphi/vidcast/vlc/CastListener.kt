package com.nuphi.vidcast.vlc

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.view.View
import arrow.core.*
import arrow.syntax.semigroup.combine
import com.nuphi.vidcast.StreamListActivity
import com.nuphi.vidcast.common.Types
import com.nuphi.vidcast.common.Types.viderror
import com.nuphi.vidcast.task.XmlParserTask
import pl.droidsonroids.casty.Casty
import pl.droidsonroids.casty.MediaData
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import javax.net.ssl.SSLHandshakeException

/// typealias CastData = Either<MediaData, String>
sealed class CastData

data class Chromecast(val m: MediaData) : CastData()
data class Youtube(val s: String) : CastData()
data class VideoStream(val s: String) : CastData()

class CastListener(val casty: Casty, val mainActivity: StreamListActivity, val sData: XmlParserTask.Companion.StreamData) : View.OnClickListener {
    private val vlcRequestCode = 42

    override fun onClick(v: View?) {
        CastMedia().execute(sData)
    }

    inner class CastMedia : AsyncTask<XmlParserTask.Companion.StreamData, Int, Either<Types.VidCastError,CastData>>() {
        var media: Option<CastData> = None

        override fun doInBackground(vararg params: XmlParserTask.Companion.StreamData): Either<Types.VidCastError, CastData> =
                synchronized<Either<Types.VidCastError,CastData>>(media) {
                    if (media is Some) {
                        return Right((media as Some<CastData>).t)
                    }
                    val streamData = params[0]
                    val inputUrl = streamData.url.url
                    val mediaOrError = when {
                        isRedirect(inputUrl) -> followRedirect(URL(inputUrl)).flatMap { createChromecastMedia(it, streamData) }
                        isYoutube(inputUrl) -> Right(Youtube(extractYoutubeID(inputUrl)))
                        streamData.url.streamType.format.equals(XmlParserTask.Companion.StreamMediaFormat.FLV) -> Right(VideoStream(inputUrl))
                        else -> createChromecastMedia(inputUrl, streamData)
                    }
                    if (mediaOrError is Either.Right) {
                        media = Some(mediaOrError.b)
                    }
                    return@synchronized mediaOrError
                }

        override fun onPostExecute(result: Either<Types.VidCastError, CastData>) = synchronized(media) {
            if (result is Either.Right) {
                if (result.b is Chromecast) {
                    castVideo((result.b as Chromecast).m).map {
                        mainActivity.reportError(it)
                    }
                }
                if (result.b is Youtube) {
                    openYoutubeLink((result.b as Youtube).s)
                }
                if (result.b is VideoStream) {
                    playVideoStream((result.b as VideoStream).s)
                }
            }
            if (result is Either.Left) {
                mainActivity.reportError(result.a)
            }
            return@synchronized
        }

        private fun createChromecastMedia(url: String, streamData: XmlParserTask.Companion.StreamData): Either<Types.VidCastError, Chromecast> =
            Either.Right(Chromecast(MediaData.Builder(url)
                    .setMediaType(MediaData.MEDIA_TYPE_MOVIE)
                    .setStreamType(MediaData.STREAM_TYPE_BUFFERED)
                    .setContentType(streamData.url.streamType.getContentTypeString())
                    .build()))

        private fun isRedirect(url: String): Boolean =
                url.startsWith("https://redirect.onlinestream.hu")

        private fun followRedirect(url: URL): Either<Types.VidCastError, String> {
            val hConn = url.openConnection() as HttpURLConnection
            try {
                hConn.connect()
            } catch (e: SSLHandshakeException) {
                if (url.protocol == "https") {
                    return followRedirect(URL(url.toString().replace("https", "http")))
                } else {
                    return Left(Types.VidCastError("SSL error", Some(e)))
                }
            } catch (e: Exception) {
                return Left(Types.VidCastError("Redirection error", Some(e)))
            }
            // TODO error handling
            val redirection = getBytes(hConn.inputStream).toString(Charset.defaultCharset())
            return Right(redirection.split("\n").filterNot { it.length == 0 }.last())
        }

        // TODO http vs https
        private fun isYoutube(url: String): Boolean =
                url.startsWith("https://www.youtube.com/watch?v=")

        private fun extractYoutubeID(url: String): String {
            return url.split("https://www.youtube.com/watch?v=").last()
        }

        private fun openYoutubeLink(youtubeID: String) {
            val intentApp = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + youtubeID))
            val intentBrowser = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + youtubeID))
            try {
                mainActivity.startActivity(intentApp)
            } catch (ex: ActivityNotFoundException) {
                mainActivity.startActivity(intentBrowser)
            }

        }

        private fun playVideoStream(s: String) {
            // TODO Tisza TV works locally in VLC but not in CC
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

        private fun castVideo(m: MediaData): Option<Types.VidCastError> {
            // TODO check if connected and report error if not
            if (casty.isConnected) {
                casty.player.loadMediaAndPlayInBackground(m)
                return None
            } else {
                return Some(viderror("Chromecast not connected."))
            }
        }

        fun getBytes(inputStream: InputStream): ByteArray {
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var read = inputStream.read(buffer, 0, buffer.size)
            while (read != -1) {
                baos.write(buffer, 0, read)
                read = inputStream.read(buffer, 0, buffer.size)
            }
            baos.flush()
            // return String(baos.toByteArray(), "UTF-8")
            return baos.toByteArray()
        }
    }
}