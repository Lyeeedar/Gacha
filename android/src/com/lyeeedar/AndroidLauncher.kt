package com.lyeeedar

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.crashlytics.android.Crashlytics
import com.lyeeedar.Util.Statics
import io.fabric.sdk.android.Fabric

class AndroidLauncher : AndroidApplication()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		Fabric.with(this, Crashlytics())

		val config = AndroidApplicationConfiguration()
		config.resolutionStrategy.calcMeasures(360, 640)
		config.disableAudio = false
		config.useImmersiveMode = true
		config.useWakelock = true

		Statics.android = true
		Statics.game = MainGame()

		initialize(Statics.game, config)

		Statics.applicationChanger = AndroidApplicationChanger()
		Statics.applicationChanger.updateApplication(Statics.applicationChanger.prefs)
	}
}
