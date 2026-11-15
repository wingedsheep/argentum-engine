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
 * "Has base power and toughness 1/1" or smaller — a set power of at most 1, with toughness unset
 * or at most 1 — or a toothless 0/2 (Illusory Wrappings, Reduce in Stature), which takes away
 * everything the creature does in combat but block a 2/2: the line the polarity reader's
 * `baseStatsPolarity` and the intent reader's one-shot `setsSmallBase` draw. A toughness-only
 * setter leaves power alone, so it shrinks nothing that attacks.
 */
internal fun SetBasePowerToughnessStatic.shrinksToSmallBase(): Boolean {
    val p = power ?: return false
    val t = toughness ?: 0
    return (p <= 1 && t <= 1) || (p == 0 && t <= 2)
}
