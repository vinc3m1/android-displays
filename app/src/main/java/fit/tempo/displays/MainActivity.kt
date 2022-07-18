package fit.tempo.displays

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import fit.tempo.displays.ui.theme.DisplaysTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : ComponentActivity() {

    private val handler by lazy(mode = NONE) { Handler(Looper.getMainLooper()) }
    private val displaysFlow by lazy(mode = NONE) { MutableStateFlow(displayManager.displays) }
    private val displayManager by lazy(mode = NONE) { getSystemService(DISPLAY_SERVICE) as DisplayManager }
    private val activityManager by lazy(mode = NONE) { getSystemService(ACTIVITY_SERVICE) as ActivityManager }
    private val displayListener by lazy(mode = NONE) {
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                displaysFlow.value = displayManager.displays
            }

            override fun onDisplayRemoved(displayId: Int) {
                displaysFlow.value = displayManager.displays
            }

            override fun onDisplayChanged(displayId: Int) {
                displaysFlow.value = displayManager.displays
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayManager.registerDisplayListener(displayListener, handler)

        setContent {
            val displays = displaysFlow.collectAsState()
            DisplaysTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Displays(displays.value, isActivityAllowed = { display ->
                        activityManager.isActivityStartAllowedOnDisplay(
                            this,
                            display.displayId,
                            Intent(
                                this,
                                SecondActivity::class.java
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { display ->
                        startActivity(
                            Intent(this, SecondActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId)
                                .toBundle()
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(displayListener)
        super.onDestroy()
    }
}

@Composable
fun DisplayItem(
    display: Display,
    isActivityAllowed: (Display) -> Boolean,
    onStartActivityClick: (Display) -> Unit = {}
) {

    var flags = ArrayList<String>()
    if (display.flags and Display.FLAG_PRESENTATION != 0) {
        flags.add("presentation")
    }
    if (display.flags and Display.FLAG_PRIVATE != 0) {
        flags.add("private")
    }
    if (display.flags and Display.FLAG_SECURE != 0) {
        flags.add("secure")
    }
    if (display.flags and Display.FLAG_ROUND != 0) {
        flags.add("round")
    }
    if (display.flags and Display.FLAG_SUPPORTS_PROTECTED_BUFFERS != 0) {
        flags.add("protected_buffers")
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        val activityAllowed = isActivityAllowed(display)

        Text(text = "${display.displayId}: ${display.name} ${display.width} x ${display.height}")
        Text(text = "flags: ${flags.joinToString(", ")}")
        Text(text = "activity start allowed: $activityAllowed")
        Button(onClick = { onStartActivityClick(display) }, enabled = activityAllowed) {
            Text(text = "Start SecondActivity")
        }
    }
}

@Composable
fun Displays(
    displays: Array<Display>,
    isActivityAllowed: (Display) -> Boolean,
    onStartActivityClick: (Display) -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        for (display in displays) {
            DisplayItem(display, isActivityAllowed, onStartActivityClick)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DisplaysPreview() {
    DisplaysTheme {
        Displays(emptyArray(), { false })
    }
}