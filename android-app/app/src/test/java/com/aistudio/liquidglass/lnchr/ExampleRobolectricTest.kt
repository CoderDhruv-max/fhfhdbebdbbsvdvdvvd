package com.aistudio.liquidglass.lnchr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.liquidglass.lnchr.data.LauncherPreferences
import com.aistudio.liquidglass.lnchr.model.DockScaleMode
import com.aistudio.liquidglass.lnchr.model.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Liquid Glass Launcher", appName)
  }

  @Test
  fun `dock scale mode persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = LauncherPreferences(context)

    val settings = LauncherSettings(dockScaleMode = DockScaleMode.EXPANDED)
    preferences.saveSettings(settings)

    val loaded = preferences.loadSettings()
    assertEquals(DockScaleMode.EXPANDED, loaded.dockScaleMode)
  }
}
