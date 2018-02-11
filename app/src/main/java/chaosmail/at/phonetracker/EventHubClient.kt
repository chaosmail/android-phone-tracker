@file:Suppress("PrivatePropertyName")

package chaosmail.at.phonetracker

import okhttp3.*
import java.io.IOException


class EventHubClient (namespace: String, eventHub: String, sas_key_name: String, sas_key: String) {

    private val SB_URL = "https://$namespace.servicebus.windows.net/$eventHub"
    private val HEADER_AUTHORIZATION = AzureUtil.getSasToken(SB_URL, sas_key_name, sas_key)
    private val HEADER_CONTENT_TYPE = "application/atom+xml;type=entry;charset=utf-8"

    private val REQ_URL = "https://$namespace.servicebus.windows.net/$eventHub/messages"
    private val REQ_TIMEOUT = "60"
    private val REQ_API_VERSION = "2014-01"

    private val client = OkHttpClient()
    private val JSON = MediaType.parse("application/json; charset=utf-8")

    private var callback: Callback? = null

    fun registerCallback(cb: Callback) {
        callback = cb
    }

    fun send(message: String) {

        val request = Request.Builder()
                .url("$REQ_URL?timeout=$REQ_TIMEOUT&api-version=$REQ_API_VERSION")
                .addHeader("Content-Type", HEADER_CONTENT_TYPE)
                .addHeader("Authorization", HEADER_AUTHORIZATION)
                .post(RequestBody.create(JSON, message))
                .build()

        val call = client.newCall(request)

        try {
            val response = call.execute()
            callback!!.onResponse(call, response)
        }
        catch (error: IOException) {
            callback!!.onFailure(call, error)
        }
    }
}