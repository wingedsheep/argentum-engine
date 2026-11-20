package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddSubtypeEffect
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * What "target land becomes an <type> in addition to its other types" means on a card whose own
 * attack names that land type — Sealock Monster's "can't attack unless defending player controls
 * an Island", or a landwalk keyword (islandwalk) on the same card.
 *
 * The type change adds a land type, so its holder only gains (a basic land type brings its mana
 * ability). What it is *for*, though, is the card's attack: handed to the defending player's land,
 * it lifts the card's own attack restriction, or makes its landwalk unblockable against them. So
 * the slot reads as aimed at an opponent's land ([TargetPolarity.HARMFUL], the opponent's side —
 * the extra mana it hands them is the price of the attack it opens), and the intent is the evasion
 * it buys. On our own land it would do nothing for the card.
 *
 * Read off the card's statics and keywords, never its name. A chosen type
 * ([AddSubtypeEffect.fromChosenValueKey]) or a type the card's attack doesn't name is not read:
 * what that change is for is the board's question.
 */
internal object LandTypeAttackEnabler {

    fun of(source: CardDefinition?, leaf: AddSubtypeEffect): Boolean {
        if (source == null || leaf.fromChosenValueKey != null || leaf.subtype.isBlank()) return false
        val subtype = leaf.subtype
        val walk = runCatching { Keyword.valueOf("${subtype.uppercase()}WALK") }.getOrNull()
        if (walk != null && walk in source.keywords) return true
        return source.script.staticAbilities.filterIsInstance<CantAttackUnless>().any { static ->
            val condition = static.condition as? Exists ?: return@any false
            static.filter.scope == Scope.Self && !condition.negate &&
                condition.player == Player.DefendingPlayer && condition.zone == Zone.BATTLEFIELD &&
                CardPredicate.IsLand in condition.filter.cardPredicates &&
                condition.filter.cardPredicates.any { it is CardPredicate.HasSubtype && it.subtype.value == subtype }
        }
    }
}
