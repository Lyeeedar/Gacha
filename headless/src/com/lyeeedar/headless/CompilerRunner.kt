package com.lyeeedar.headless

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.lyeeedar.MainGame
import com.lyeeedar.ResourceProcessors.AtlasCreator
import com.lyeeedar.ResourceProcessors.TextureCompressor
import com.lyeeedar.ResourceProcessors.XmlCompressor
import com.lyeeedar.ResourceProcessors.XmlLoadTester
import com.lyeeedar.Util.Statics
import org.mockito.Mockito

object CompilerRunner
{
	@JvmStatic fun main(arg: Array<String>)
	{
		Gdx.gl = Mockito.mock(GL20::class.java)
		Gdx.gl20 = Mockito.mock(GL20::class.java)
		Statics.game = MainGame(false)
		Gdx.app = HeadlessApplication(Statics.game)

		println("##########################################################")

		AtlasCreator()
		TextureCompressor()
		XmlCompressor()
		XmlLoadTester.test()

		println("##########################################################")

		Gdx.app.exit()
	}
}
