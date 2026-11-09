package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Binds the unbound "of the chosen ..." restrictions to what the producing source chose as it
 * entered, turning them into self-contained restrictions the pool can hold:
 *  - [ManaRestriction.OfChosenType] -> [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] with the chosen
 *    creature type (Cavern of Souls);
 *  - [ManaRestriction.SpellsOfChosenColor] -> [ManaRestriction.SpellsWithExactColorCount] with the
 *    chosen color (Throne of Eldraine's "monocolored spells of that color").
 * Binding also reaches inside [ManaRestriction.AnyOf] / [ManaRestriction.AllOf] (The Ringhart Crest:
 * "a creature spell of the chosen type or a legendary creature spell").
 *
 * Every place that turns a mana effect's restriction into pool mana, or into a solver source,
 * calls this with the source that is making the mana, so the pool never holds an unbound form.
 * A source that made no matching choice leaves it unbound, which no payment satisfies.
 */
object ChosenTypeManaRestriction {

    fun bind(restriction: ManaRestriction?, state: GameState, sourceId: EntityId?): ManaRestriction? {
        if (restriction == null || !restriction.isUnbound()) return restriction
        val entity = sourceId?.let { state.getEntity(it) }
        return restriction.bindTo(entity?.chosenCreatureType(), entity?.chosenColor())
    }

    private fun ManaRestriction.isUnbound(): Boolean = when (this) {
        is ManaRestriction.OfChosenType, is ManaRestriction.SpellsOfChosenColor -> true
        is ManaRestriction.AnyOf -> restrictions.any { it.isUnbound() }
        is ManaRestriction.AllOf -> restrictions.any { it.isUnbound() }
        else -> false
    }

    private fun ManaRestriction.bindTo(chosenType: String?, chosenColor: Color?): ManaRestriction = when (this) {
        is ManaRestriction.OfChosenType ->
            if (chosenType == null) this else ManaRestriction.SubtypeSpellsOrAbilitiesOnly(chosenType, creatureOnly)
        is ManaRestriction.SpellsOfChosenColor ->
            if (chosenColor == null) this else ManaRestriction.SpellsWithExactColorCount(exactColorCount, chosenColor)
        is ManaRestriction.AnyOf -> copy(restrictions = restrictions.map { it.bindTo(chosenType, chosenColor) })
        is ManaRestriction.AllOf -> copy(restrictions = restrictions.map { it.bindTo(chosenType, chosenColor) })
        else -> this
    }
}
