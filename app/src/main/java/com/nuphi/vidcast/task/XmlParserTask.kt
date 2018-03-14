package com.nuphi.vidcast.task

import android.os.AsyncTask
import arrow.core.Either
import arrow.core.Some
import arrow.data.Try
import com.nuphi.vidcast.common.Types.VidCastError
import org.w3c.dom.Element
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import com.nuphi.vidcast.task.XmlParserTask.Companion.StreamData
import java.io.ByteArrayInputStream

class XmlParserTask(val addItem: (s: StreamData) -> Unit, val handleResult: (l: List<StreamData>) -> Unit, val onError: (s: VidCastError) -> Unit) : AsyncTask<ByteArray, Int, Either<VidCastError, List<StreamData>>>() {
    companion object {
        data class StreamData(val title: String, val url: StreamUrlData, val picUrl: String) {
            override fun toString(): String = title
        }

        data class StreamUrlData(val url: String, val streamType: StreamType)
        data class StreamType(val mediaType: StreamMediaType, val format: StreamMediaFormat, val protocol: StreamProtocol) {
            // TODO this can be done simpler
            fun getContentTypeString(): String {
                val mT = when (mediaType) {
                    StreamMediaType.AUDIO -> "audio"
                    StreamMediaType.VIDEO -> "video"
                }
                return arrayListOf(mT, format.toString()).joinToString("/")
            }
        }

        enum class StreamMediaFormat { WEBM, AAC, MP4, WAV, FLV, OTHER;

            override fun toString(): String =
                    when (this) {
                        StreamMediaFormat.OTHER -> ""
                        else -> super.toString()
                    }
        }

        fun readStreamMediaFormat(s: String): StreamMediaFormat {
            try {
                val r = when(s) {
                    "AAC+" -> StreamMediaFormat.AAC
                    "MPEGTS" -> StreamMediaFormat.MP4
                    "WMV" -> StreamMediaFormat.MP4
                    else -> StreamMediaFormat.valueOf(s)
                }
                return r
            } catch (e: Exception) {
                return StreamMediaFormat.OTHER
            }
        }

        enum class StreamMediaType { VIDEO, AUDIO }
        enum class StreamProtocol { HLS, COMMON }
    }

    override fun doInBackground(vararg xmls: ByteArray?): Either<VidCastError, List<StreamData>> {
        return Try.invoke<List<StreamData>> {
            val xml = xmls[0]
            // download the file
            // TODO extract a function
            val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val dom = parser.parse(ByteArrayInputStream(xml))
            val stations = dom.getElementsByTagName("station")
            val results = LinkedList<StreamData>()
            for (i in 0..stations.length - 1) {
                val station = stations.item(i) as Element
                val description = station.getElementsByTagName("title").item(0) as Element
                val logo = getChildValue(station, "logo", "")
                val title = getChildValue(station, "title", "Untitled ${i}")
                val isTv = getChildValue(station, "tv", "0")
                val streamUrlData = getFirstChannelUrl(station)
                // TODO Currently, we only allow TVs
                if (streamUrlData != null && isTv.equals("1")) {
                    results.add(StreamData(title, streamUrlData, logo))
                }
            }
            return Either.Right(results)
        }.toEither().mapLeft {
            VidCastError(it.message.orEmpty(), Some(it))
        }
    }

    override fun onPostExecute(result: Either<VidCastError, List<StreamData>>) {
        super.onPostExecute(result)
        if (result is Either.Right) {
            result.b.map {
                addItem(it)
            }
            handleResult(result.b)
        } else if (result is Either.Left) {
            onError(result.a)
        }
    }

    private fun getChildValue(node: Element, childTagName: String, default: String): String {
        val description = node.getElementsByTagName(childTagName).item(0) as Element
        var result = default
        if (description.childNodes.length > 0) {
            result = description.firstChild.nodeValue
        }
        return result
    }

    private fun getFirstChannelUrl(station: Element): StreamUrlData? {
        val channels = station.getElementsByTagName("channel")
        if (channels.length > 0) {
            val firstChan = channels.item(0) as Element
            val streamUrl = getChildValue(firstChan, "stream_url", "")
            val formatStr = getChildValue(firstChan, "format", "other").toUpperCase()
            val format = readStreamMediaFormat(formatStr)
            if (!streamUrl.isNullOrEmpty()) {
                return StreamUrlData(streamUrl, StreamType(StreamMediaType.VIDEO, format, StreamProtocol.COMMON))
            }
            val hlsUrl = getChildValue(firstChan, "hls_url", "")
            if (!hlsUrl.isNullOrEmpty()) {
                return StreamUrlData(hlsUrl, StreamType(StreamMediaType.VIDEO, format, StreamProtocol.HLS))
            }
        }
        return null
    }
}