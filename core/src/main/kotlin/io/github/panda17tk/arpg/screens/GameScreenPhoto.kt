package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.ui.Modals
import io.github.panda17tk.arpg.ui.Overlay
import io.github.panda17tk.arpg.ui.PhotoCam

/**
 * v2.187 写真モード — an in-engine framing mode opened from a space pause: the world holds still
 * (the Overlay register freezes the sim for free), the HUD steps aside, and the camera pans (drag)
 * and zooms (遠/近 chips) so a clean shot can be composed with the device's own screenshot.
 * No capture, no platform code — purely camera framing + HUD suppression.
 */
internal fun GameScreen.handlePhotoTaps() {
    if (!tapped) return
    when (Modals.hitModal(PhotoCam.buttons(hudViewport.worldWidth, hudViewport.worldHeight), tapX, tapY)) {
        0 -> overlay = Overlay.NONE // × — close; the follow lerps back to the keeper next frame
        1 -> photoZoom = PhotoCam.zoomBy(photoZoom, PhotoCam.ZOOM_STEP) // 遠 — pull back (wider)
        2 -> photoZoom = PhotoCam.zoomBy(photoZoom, -PhotoCam.ZOOM_STEP) // 近 — push in (closer)
        else -> {}
    }
}

/** Drag one finger to pan the framed view — converted to world units so it tracks under the finger. */
internal fun GameScreen.pollPhotoInput() {
    if (Gdx.input.isTouched(0)) {
        val cx = Gdx.input.getX(0).toFloat(); val cy = Gdx.input.getY(0).toFloat()
        if (photoDragActive) {
            tmpTap.set(photoDragX, photoDragY, 0f); worldViewport.unproject(tmpTap)
            val wpx = tmpTap.x; val wpy = tmpTap.y
            tmpTap.set(cx, cy, 0f); worldViewport.unproject(tmpTap)
            camX = PhotoCam.clamp(camX + (wpx - tmpTap.x), 0f, gw.map.width * Tuning.TILE)
            camY = PhotoCam.clamp(camY + (wpy - tmpTap.y), 0f, gw.map.height * Tuning.TILE)
        }
        photoDragX = cx; photoDragY = cy; photoDragActive = true
    } else {
        photoDragActive = false
    }
}

/** The corner chips over the still world — the only UI a framed shot carries. */
internal fun GameScreen.drawPhotoOverlay() {
    hudViewport.apply()
    val btns = PhotoCam.buttons(hudViewport.worldWidth, hudViewport.worldHeight)
    Gdx.gl.glEnable(GL20.GL_BLEND)
    shapes.projectionMatrix = hudViewport.camera.combined
    shapes.begin(ShapeRenderer.ShapeType.Filled)
    btns.forEach { b ->
        cEventTmp.set(0.55f, 0.75f, 1f, 0.20f); shapes.color = cEventTmp
        shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
        cEventTmp.set(0.06f, 0.08f, 0.12f, 0.82f); shapes.color = cEventTmp
        shapes.rect(b.x, b.y, b.w, b.h)
    }
    shapes.end()
    batch.projectionMatrix = hudViewport.camera.combined
    batch.begin()
    cEventTmp.set(0.90f, 0.93f, 1f, 1f); font.color = cEventTmp
    btns.forEach { b ->
        bannerGlyph.setText(font, b.label)
        font.draw(batch, bannerGlyph, b.centerX - bannerGlyph.width / 2f, b.centerY + bannerGlyph.height / 2f)
    }
    font.color = Color.WHITE
    batch.end()
}
