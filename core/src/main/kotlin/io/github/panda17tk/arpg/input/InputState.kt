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
    var dash = false
    var placeWall = false   // edge-triggered: true only on the frame F transitions down
    var fire = false           // held (K)
    var melee = false          // edge (J)
    var reload = false         // edge (R)
    var weaponSlot = -1        // 0..4 on the frame 1-5 is pressed, else -1
    var aimX = 0f              // twin-stick: right-stick aim direction (unit vector)
    var aimY = 0f
    var aiming = false         // true while the right stick is pushed → face + auto-fire along aim
}
