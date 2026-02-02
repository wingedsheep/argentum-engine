package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Filter for spell types that can be countered.
 */
@Serializable
sealed interface SpellFilter {
    val description: String

    @Serializable
    data object AnySpell : SpellFilter {
        override val description: String = ""
    }

    @Serializable
    data object CreatureSpell : SpellFilter {
        override val description: String = "creature"
    }

    @Serializable
    data object NonCreatureSpell : SpellFilter {
        override val description: String = "noncreature"
    }

    @Serializable
    data object SorcerySpell : SpellFilter {
        override val description: String = "sorcery"
    }

    @Serializable
    data object InstantSpell : SpellFilter {
        override val description: String = "instant"
    }

    @Serializable
    data class CreatureOrSorcery(val dummy: Unit = Unit) : SpellFilter {
        override val description: String = "creature or sorcery"
    }
}
