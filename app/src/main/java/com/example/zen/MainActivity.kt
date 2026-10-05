package com.example.zen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.zen.data.ZenPrefs
import com.example.zen.persona.PersonaPalette

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    applyPersonaSystemBars(PersonaPalette.of(ZenPrefs.peekPersona(this)).isLight)
    setContent { ZenApp() }
  }
}
