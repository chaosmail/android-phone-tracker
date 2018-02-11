package chaosmail.at.phonetracker

import android.annotation.SuppressLint
import android.location.LocationManager
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.*

data class Record(val device: NormalizedDeviceInfo?,
        val network: List<NormalizedCellInfo>, val location: NormalizedLocationInfo?)

@Suppress("PrivatePropertyName")
class MainActivity : AppCompatActivity(), Callback {
    enum class State { RUNNING, STOPPED }

    private val TAG = "PhoneTracker"
    private var currentState = State.STOPPED
    lateinit var eventHub: EventHubClient
    lateinit var networkInfo: NetworkInfo
    lateinit var locationInfo: LocationInfo
    lateinit var deviceInfo: DeviceInfo

    private lateinit var textView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var inputFrequency: EditText

    private val KEY_RESULT_HANDLER = "PhoneTracker::UIUpdate"

    private val resultHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            setResultState(msg.data.getBoolean(KEY_RESULT_HANDLER))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        textView = findViewById(R.id.dotBox)
        startButton = findViewById(R.id.startButton)
        progressBar = findViewById(R.id.progressBar)
        stopButton = findViewById(R.id.stopButton)
        inputFrequency = findViewById(R.id.inputFrequency)

        eventHub = EventHubClient(
                getString(R.string.azure_namespace),
                getString(R.string.azure_event_hub),
                getString(R.string.azure_sas_key_name),
                getString(R.string.azure_sas_key))

        // Register this activity as Http Callback
        eventHub.registerCallback(this)

        networkInfo = NetworkInfo(this, getSystemService(TELEPHONY_SERVICE) as TelephonyManager)
        locationInfo = LocationInfo(this, getSystemService(LOCATION_SERVICE) as LocationManager)
        deviceInfo = DeviceInfo(this, getSystemService(TELEPHONY_SERVICE) as TelephonyManager)
    }

    private fun setAppState(state: State) {
        currentState = state
        when (state) {
            State.RUNNING -> {
                textView.text = ""
                startButton.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE
                stopButton.visibility = View.VISIBLE
            }
            State.STOPPED -> {
                startButton.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
                stopButton.visibility = View.INVISIBLE
            }
        }
    }

    fun setResultState(result: Boolean) {
        when(result) {
            true -> textView.append(".")
            false -> textView.append("E")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun startRecording(view: View) {
        setAppState(state = State.RUNNING)

        val period: Long = inputFrequency.text.toString().toLong()

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val msg = Record(deviceInfo.getDeviceInfo(),
                        networkInfo.getAllCells(), locationInfo.getLocation())
                val msgJson = Gson().toJson(msg)
                Log.d(TAG, msgJson)
                eventHub.send(msgJson)

                if (currentState == State.STOPPED) {
                    this.cancel()
                }
            }
        }, 0, 1000 * period) // put here time 1000 milliseconds = 1 second
    }

    @Suppress("UNUSED_PARAMETER")
    fun stopRecording(view: View) {
        setAppState(state = State.STOPPED)
    }

    private fun notifyResultHandler(value: Boolean, key: String = KEY_RESULT_HANDLER) {
        val msg = resultHandler.obtainMessage()
        val bundle = Bundle()
        bundle.putBoolean(key, value)
        msg.data = bundle
        resultHandler.sendMessage(msg)
    }

    override fun onFailure(call: Call?, e: IOException?) {
        Log.e(TAG, "Http Request failed")
    }

    override fun onResponse(call: Call?, response: Response?) {
        when {
            response != null -> notifyResultHandler(response.isSuccessful)
            else -> Log.e(TAG, "Http Response null")
        }
        Log.d(TAG, response.toString())
    }
}