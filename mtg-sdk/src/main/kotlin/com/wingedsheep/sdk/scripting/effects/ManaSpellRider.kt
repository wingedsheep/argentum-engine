package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A side-effect that attaches to a spell when mana carrying this rider is spent on it.
 *
 * Riders are orthogonal to [ManaRestriction]:
 *  - A [ManaRestriction] controls *where* mana may be spent.
 *  - A [ManaSpellRider] controls *what happens to the spell* when the mana is spent.
 *
 * The set of riders consumed during payment is collected by the cast pipeline and
 * applied to the spell as it goes on the stack.
 */
@Serializable
sealed interface ManaSpellRider {
    val description: String

    /**
     * "That spell can't be countered." (Cavern of Souls)
     *
     * Translates to stamping `CantBeCounteredComponent` on the spell at cast time.
     */
    @SerialName("MakesSpellUncounterable")
    @Serializable
    data object MakesSpellUncounterable : ManaSpellRider {
        override val description: String = "That spell can't be countered"
    }
}
