package com.nuphi.vidcast.task

import android.os.AsyncTask
import arrow.core.Either
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import com.nuphi.vidcast.common.Types.VidCastError

class DownloadTask(val onSuccess: (r: ByteArray) -> Unit, val onError: (r: VidCastError) -> Unit) : AsyncTask<String, Int, Either<VidCastError, ByteArray>>() {
    override fun doInBackground(vararg urls: String?): Either<VidCastError, ByteArray> {
        var connection: HttpURLConnection? = null
        try {
            val urlStr = urls[0]
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return error("Server returned HTTP " + connection.responseCode
                        + " " + connection.responseMessage)
            }

            // getBytes the file
            return Either.Right(getBytes(connection.inputStream))
        } catch (e: Exception) {
            return error(e.toString())
        } finally {
            if (connection != null) {
                connection.disconnect()
            }
        }
        return error("An unknown error happened.")
    }

    override fun onPostExecute(result: Either<VidCastError, ByteArray>) {
        if (result is Either.Right) {
            onSuccess(result.b)
        } else if (result is Either.Left) {
            onError(result.a)
        }
    }

    companion object {
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