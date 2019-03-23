package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Global
import com.lyeeedar.Systems.render
import com.lyeeedar.Util.AssetManager

class RenderSystemWidget : Widget()
{
	val white = AssetManager.loadTextureRegion("GUI/border")

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		Global.engine.render().doRender(batch, this)
	}
}