package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** A creature's role in its tribe's family (Living Planets). Drives protection and morale. */
@Serializable
enum class FamilyRole { NONE, CHILD, ELDER, GUARDIAN, KING }
