package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

class SpaceDriveTest {
    private val dt = 1f / 60f
    private val cruise = 100f
    private val walk = 600f
    private val stick = 300f
    private val button = 2000f
    private val hardCap = 1000f

    private fun walkStep(vx: Float, vy: Float, dx: Float, dy: Float, decay: Float = 1f) =
        SpaceDrive.step(vx, vy, dx, dy, SpaceDrive.Mode.WALK, walk, stick, button, cruise, decay, hardCap, dt)

    // ---- mode() ----

    @Test fun `the dash button wins, even standing still`() {
        assertEquals(SpaceDrive.Mode.BUTTON_DASH, SpaceDrive.mode(moving = false, stickMag = 0f, dashButton = true, canDash = true, stickDashMin = 0.85f))
    }

    @Test fun `a hard stick push just walks now (stick auto-dash removed)`() {
        assertEquals(SpaceDrive.Mode.WALK, SpaceDrive.mode(moving = true, stickMag = 0.9f, dashButton = false, canDash = true, stickDashMin = 0.85f))
    }

    @Test fun `a gentle push just walks`() {
        assertEquals(SpaceDrive.Mode.WALK, SpaceDrive.mode(moving = true, stickMag = 0.5f, dashButton = false, canDash = true, stickDashMin = 0.85f))
    }

    @Test fun `no stamina means a big push or held button falls back to a walk`() {
        assertEquals(SpaceDrive.Mode.WALK, SpaceDrive.mode(moving = true, stickMag = 1f, dashButton = true, canDash = false, stickDashMin = 0.85f))
    }

    @Test fun `standing idle coasts`() {
        assertEquals(SpaceDrive.Mode.NONE, SpaceDrive.mode(moving = false, stickMag = 0f, dashButton = false, canDash = true, stickDashMin = 0.85f))
    }

    // ---- step(): zero friction ----

    @Test fun `coasting in space loses no speed`() {
        val (nx, ny) = SpaceDrive.step(300f, 0f, 0f, 0f, SpaceDrive.Mode.NONE, walk, stick, button, cruise, decay = 1f, hardCap = hardCap, dt = dt)
        assertEquals(300f, nx, 1e-3f)
        assertEquals(0f, ny, 1e-3f)
    }

    @Test fun `ground friction bleeds a coast off`() {
        val (nx, _) = SpaceDrive.step(300f, 0f, 0f, 0f, SpaceDrive.Mode.NONE, walk, stick, button, cruise, decay = 0.5f, hardCap = hardCap, dt = 1f)
        assertEquals(150f, nx, 1e-3f) // halves in a second on the ground
    }

    // ---- step(): walk governor (the normal-move speed cap) ----

    @Test fun `walking from rest tops out at the cruise cap`() {
        var vx = 0f; var vy = 0f
        repeat(120) { val (nx, ny) = walkStep(vx, vy, 1f, 0f); vx = nx; vy = ny }
        assertEquals(cruise, hypot(vx, vy), 1e-2f)
    }

    @Test fun `a halved walk cap tops out at half cruise (the space normal-move throttle)`() {
        var vx = 0f; var vy = 0f
        repeat(120) {
            val (nx, ny) = SpaceDrive.step(vx, vy, 1f, 0f, SpaceDrive.Mode.WALK, walk, stick, button, cruise, 1f, hardCap, dt, walkCapMul = 0.5f)
            vx = nx; vy = ny
        }
        assertEquals(cruise * 0.5f, hypot(vx, vy), 1e-2f)
    }

    @Test fun `the halved walk cap does not throttle a dash (dash-ram preserved)`() {
        // BUTTON_DASH ignores walkCapMul — still climbs to 3× cruise even when normal move is throttled.
        var vx = 0f; var vy = 0f
        repeat(200) {
            val (nx, ny) = SpaceDrive.step(vx, vy, 1f, 0f, SpaceDrive.Mode.BUTTON_DASH, walk, stick, button, cruise, 1f, hardCap, dt, walkCapMul = 0.5f)
            vx = nx; vy = ny
        }
        assertEquals(cruise * 3f, hypot(vx, vy), 1e-2f)
    }

    @Test fun `walking forward never speeds up a faster coast`() {
        val (nx, ny) = walkStep(300f, 0f, 1f, 0f) // already well above cruise
        assertEquals(300f, hypot(nx, ny), 1e-3f) // governor holds it — no free acceleration past a dash
    }

    @Test fun `walking backward retro-burns a fast coast down`() {
        val (nx, _) = walkStep(300f, 0f, -1f, 0f)
        assertTrue(nx < 300f, "reverse thrust should bleed speed: $nx")
        assertEquals(300f - walk * dt, nx, 1e-2f)
    }

    // ---- step(): dashes ignore the cruise cap, obey the hard cap ----

    @Test fun `a button dash accelerates past the cruise cap`() {
        var vx = 0f; var vy = 0f
        repeat(20) {
            val (nx, ny) = SpaceDrive.step(vx, vy, 1f, 0f, SpaceDrive.Mode.BUTTON_DASH, walk, stick, button, cruise, 1f, hardCap, dt)
            vx = nx; vy = ny
        }
        assertTrue(hypot(vx, vy) > cruise * 2f, "button dash should blow past cruise: ${hypot(vx, vy)}")
    }

    @Test fun `a button dash tops out at three times cruise`() {
        var vx = 0f; var vy = 0f
        repeat(200) { val (nx, ny) = SpaceDrive.step(vx, vy, 1f, 0f, SpaceDrive.Mode.BUTTON_DASH, walk, stick, button, cruise, 1f, hardCap, dt); vx = nx; vy = ny }
        assertEquals(cruise * 3f, hypot(vx, vy), 1e-2f)
    }

    @Test fun `a dash never clamps away faster inertia you already built`() {
        val fast = cruise * 5f // e.g. flung fast by a gravity slingshot
        val (nx, ny) = SpaceDrive.step(fast, 0f, 1f, 0f, SpaceDrive.Mode.BUTTON_DASH, walk, stick, button, cruise, 1f, hardCap, dt)
        assertTrue(hypot(nx, ny) >= fast - 1e-2f, "a dash must not slow a faster coast: ${hypot(nx, ny)} vs $fast")
    }

    @Test fun `nothing exceeds the absolute hard cap`() {
        val (nx, ny) = SpaceDrive.step(5000f, 0f, 0f, 0f, SpaceDrive.Mode.NONE, walk, stick, button, cruise, 1f, hardCap, dt)
        assertEquals(hardCap, hypot(nx, ny), 1e-2f)
    }
}
