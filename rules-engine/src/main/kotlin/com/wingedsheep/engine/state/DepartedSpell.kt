package com.wingedsheep.engine.state

import kotlinx.serialization.Serializable

/**
 * A spell as it last existed on the stack (CR 608.2h), kept after it left without resolving.
 * [objectRef] is the stack object that left, so a reference captured while it was on the stack
 * (a cast trigger's triggering spell) can tell it apart from a later object of the same card.
 */
@Serializable
data class DepartedSpell(val objectRef: ObjectRef?, val spell: ComponentContainer)
