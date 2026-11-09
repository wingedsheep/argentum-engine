package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * What "target creature becomes a <type>" means on a card whose own static names that type on one
 * side of a blocking restriction — "<blockers> can't block <attackers>", a [CantBeBlockedBy] whose
 * blocker filter and protected-creature filter each name a creature subtype ("Cowards can't block
 * Warriors": Boldwyr Intimidator, Kargan Intimidator, Gornog, the Red Reaper).
 *
 * A [SetCreatureSubtypesEffect] replaces the creature's other creature types, so the type it sets is
 * the whole of what it does, and the card's restriction says what that type is worth:
 *
 *  - **The blocker type** ("becomes a Coward"): the creature can no longer block the creatures the
 *    restriction protects — the single-creature can't-block, a curse on its holder and an evasion
 *    grant for our attack, aimed at the opponent's blocker.
 *  - **The protected type** ("becomes a Warrior"): the creature can't be blocked by the blocker
 *    type — an evasion grant, a gift aimed at our attacker.
 *
 * Read off the card's statics, never its name. A type both sides name, a chosen type
 * ([SetCreatureSubtypesEffect.fromChosenValueKey]) or a card with no such static is not read: what a
 * type change is for elsewhere (typal lords, a sweeper that names a type) is the board's question.
 */
internal object TypeEvasion {

    enum class Side(val polarity: TargetPolarity) {
        /** The type the restriction keeps from blocking: the creature loses its blocks. */
        CANT_BLOCK(TargetPolarity.HARMFUL),

        /** The type the restriction protects: the creature evades the blocker type. */
        EVADES(TargetPolarity.BENEFICIAL),
    }

    fun of(source: CardDefinition?, leaf: SetCreatureSubtypesEffect): Side? {
        if (source == null || leaf.fromChosenValueKey != null || leaf.subtypes.isEmpty()) return null
        val restrictions = source.script.staticAbilities.filterIsInstance<CantBeBlockedBy>()
        if (restrictions.isEmpty()) return null
        val blockers = restrictions.flatMapTo(HashSet()) { subtypesOf(it.blockerFilter) }
        val protected = restrictions.flatMapTo(HashSet()) { subtypesOf(it.filter.baseFilter) }
        val blocks = leaf.subtypes.any { it in blockers }
        val evades = leaf.subtypes.any { it in protected }
        return when {
            blocks && !evades -> Side.CANT_BLOCK
            evades && !blocks -> Side.EVADES
            else -> null
        }
    }

    private fun subtypesOf(filter: GameObjectFilter): List<String> =
        filter.cardPredicates.filterIsInstance<CardPredicate.HasSubtype>().map { it.subtype.value }
}
