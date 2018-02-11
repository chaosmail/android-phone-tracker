package chaosmail.at.phonetracker

import android.util.Base64
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


object AzureUtil {

    fun getSasToken(resourceUri: String, keyName: String, key: String): String {
        val epoch = System.currentTimeMillis() / 1000L
        val week = 60 * 60 * 24 * 7
        val expiry = java.lang.Long.toString(epoch + week)
        val stringToSign = URLEncoder.encode(resourceUri, "UTF-8") + "\n" + expiry
        val signature = getHmac256(key, stringToSign)
        val encUri = URLEncoder.encode(resourceUri, "UTF-8")
        val encSig = encodeSignature(signature)

        return "SharedAccessSignature sr=$encUri&sig=$encSig&se=$expiry&skn=$keyName"
    }

    private fun encodeSignature(sig: String?): String {
        return URLEncoder.encode(sig, "UTF-8").replace("%2F", "/")
    }

    private fun getHmac256(key: String, input: String): String? {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        sha256Hmac!!.init(secretKey)
        val hash = sha256Hmac.doFinal(input.toByteArray(charset("UTF-8")))
        return String(Base64.encode(hash, Base64.NO_WRAP))
    }
}