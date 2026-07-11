package io.github.panda17tk.arpg.input

/**
 * Unified per-frame input snapshot. PC keyboard booleans for now; Phase 8 adds
 * analog move/aim and autoFire for on-screen touch sticks.
 */
class InputState {
    var left = false
    var right = false
    var up = false
    var down = false
    var moveMag = 0f           // analog move-stick deflection 0..1 (keyboard stays 0); a big push triggers a stick dash
    var dash = false
    var placeWallT = 0f
    var placeWall: Boolean  // edge-triggered, buffered (v2.153)
        get() = placeWallT > 0f
        set(value) { placeWallT = if (value) EDGE_BUFFER else 0f }
    var fire = false           // held (K)

    // v2.42: the manual-fire release is a BUFFERED request, not a one-frame edge. A render frame can
    // run zero fixed sim steps, and the release can land mid-cooldown/reload — a raw edge got
    // swallowed in both cases (the "aimed, released, nothing fired" bug). The buffer holds the
    // request ~0.3s until FireSystem consumes it (or it decays).
    var fireReleaseT = 0f
    var fireRelease: Boolean
        get() = fireReleaseT > 0f
        set(value) { fireReleaseT = if (value) FIRE_BUFFER else 0f }
    // v2.153 音と手の修理: the other edge actions get the fireRelease treatment — a render
    // frame that runs zero sim substeps must not swallow a tap. Consumers clear on use.
    var meleeT = 0f
    var melee: Boolean
        get() = meleeT > 0f
        set(value) { meleeT = if (value) EDGE_BUFFER else 0f }
    // v2.112 エイム補助: set by the screen from settings. Input IS the nondeterministic boundary,
    // so the sim reading this flag keeps the determinism contract intact.
    var aimAssist = true
    var reloadT = 0f
    var reload: Boolean
        get() = reloadT > 0f
        set(value) { reloadT = if (value) EDGE_BUFFER else 0f }
    var land = false           // edge: land on / take off from a planet (L key or the touch LAND button)
    var weaponSlot = -1        // 0..4 on the frame 1-5 is pressed, else -1
    var aimX = 0f              // twin-stick: right-stick aim direction (unit vector)
    var aimY = 0f
    var aiming = false         // true while the right stick is pushed → face + auto-fire along aim
    var inventory = false      // edge: open/close the inventory screen (I key or the INV button) — v2.33
    var tune = false           // edge: open/close the tuning popup (調整 button) — v2.98
    var fullThrottle = false   // held: OC thruster full throttle (O key or the FULL button) — v2.33

    companion object {
        const val FIRE_BUFFER = 0.3f // seconds a manual-fire release stays queued (v2.42)
        const val EDGE_BUFFER = 0.25f // v2.153: melee/reload/wall taps queue the same way
    }
}
