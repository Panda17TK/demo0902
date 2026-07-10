package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/**
 * v2.143 調整モード・野生の棚 — the wildlife/boid knobs the tuning overlay can reach.
 * The systems read these live (SchoolFishSystem, WildPredationSystem); defaults match the
 * shipped v2.131–v2.138 constants exactly.
 */
@Serializable
data class WildConfig(
    var schoolCohesion: Float = 0.9f,  // boids: pull toward the school's centre
    var schoolAlign: Float = 0.7f,     // boids: match the neighbours' heading
    var schoolSeparate: Float = 26f,   // boids: push apart when packed
    var schoolFlee: Float = 3.2f,      // boids: bolt from the keeper
    var schoolWander: Float = 0.35f,   // boids: the calm shimmer
    var biteWindup: Float = 0.45f,     // v2.138: the brave hunter's lunge telegraph (keep > 0)
)
