package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Binds [ManaRestriction.OfChosenType] to the creature type the producing source chose as it
 * entered, turning it into a self-contained [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] —
 * including inside [ManaRestriction.AnyOf] / [ManaRestriction.AllOf] (The Ringhart Crest: "a
 * creature spell of the chosen type or a legendary creature spell").
 *
 * Every place that turns a mana effect's restriction into pool mana, or into a solver source,
 * calls this with the source that is making the mana, so the pool never holds the unbound form.
 * A source with no chosen type leaves it unbound, which no payment satisfies.
 */
object ChosenTypeManaRestriction {

    fun bind(restriction: ManaRestriction?, state: GameState, sourceId: EntityId?): ManaRestriction? {
        if (restriction == null || !restriction.mentionsChosenType()) return restriction
        val chosenType = sourceId?.let { state.getEntity(it)?.chosenCreatureType() } ?: return restriction
        return restriction.bindTo(chosenType)
    }

    private fun ManaRestriction.mentionsChosenType(): Boolean = when (this) {
        is ManaRestriction.OfChosenType -> true
        is ManaRestriction.AnyOf -> restrictions.any { it.mentionsChosenType() }
        is ManaRestriction.AllOf -> restrictions.any { it.mentionsChosenType() }
        else -> false
    }

    private fun ManaRestriction.bindTo(chosenType: String): ManaRestriction = when (this) {
        is ManaRestriction.OfChosenType -> ManaRestriction.SubtypeSpellsOrAbilitiesOnly(chosenType, creatureOnly)
        is ManaRestriction.AnyOf -> copy(restrictions = restrictions.map { it.bindTo(chosenType) })
        is ManaRestriction.AllOf -> copy(restrictions = restrictions.map { it.bindTo(chosenType) })
        else -> this
    }
}
