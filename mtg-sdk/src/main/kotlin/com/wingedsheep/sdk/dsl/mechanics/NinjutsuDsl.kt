package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add Ninjutsu [cost] (CR 702.49).
 *
 * "[cost], Return an unblocked attacker you control to hand: Put this card onto the battlefield
 * from your hand tapped and attacking."
 *
 * Display-only at the DSL layer — all behavior lives in the engine's shared declare-blockers
 * alternative-cost pipeline (the same one that drives [com.wingedsheep.sdk.dsl.sneak]), which keys
 * off [KeywordAbility.ninjutsuStyleCost]: the legal-action enumerator surfaces the cast only during
 * the active player's combat once blocked/unblocked status is assigned and they control an
 * unblocked attacker, the cast handler charges the ninjutsu mana plus returns the chosen unblocked
 * attacker to hand, and a resolving permanent enters tapped and attacking the same defender
 * (CR 506.3a). A card that isn't a creature as it enters (e.g. an un-animated planeswalker) just
 * enters tapped.
 */
fun CardBuilder.ninjutsu(cost: String) {
    keywordAbilityList.add(KeywordAbility.ninjutsu(cost))
}
