package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val ENCHANTED_PLAYER_CONTROLS =
    ControllerPredicate.ControlledByReferencedPlayer(EffectTarget.PlayerRef(Player.EnchantedPlayer))

/** "… enchanted player controls": an "Enchant player" Aura reaching that player's permanents. */
internal fun GroupFilter.reachesEnchantedPlayer(): Boolean =
    baseFilter.anyOf.isEmpty() && baseFilter.controllerPredicate == ENCHANTED_PLAYER_CONTROLS

/**
 * "Has base power and toughness 1/1" or smaller: a set power of at most 1, with toughness unset
 * or at most 1. A toughness-only setter leaves power alone, so it shrinks nothing that attacks.
 */
internal fun SetBasePowerToughnessStatic.shrinksToAtMostOneOne(): Boolean {
    val p = power ?: return false
    return p <= 1 && (toughness ?: 0) <= 1
}
