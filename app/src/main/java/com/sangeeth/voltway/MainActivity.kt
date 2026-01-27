package com.sangeeth.voltway

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.common.MapboxOptions
import com.mapbox.search.autocomplete.PlaceAutocomplete
import com.sangeeth.voltway.ui.theme.VoltWayTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)
        val placeAutocomplete = PlaceAutocomplete.create(locationProvider = null)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED){
                val response = placeAutocomplete.suggestions(query = "New York")

                if (response.isValue){
                    val suggestions = response.value.orEmpty()
                    Log.i("SearchExample", "Suggestions: $suggestions")
                    if (suggestions.isNotEmpty()) {
                        val result = placeAutocomplete.select(suggestions.first())
                        result.onValue { Log.i("SearchExample", "Result: $it") }
                        result.onError { Log.e("SearchExample", "Error selecting suggestion", it) }
                    }

                }else {
                    Log.e("SearchExample", "Error fetching suggestions: ${response.error}")
                }
            }
        }
    }
}

