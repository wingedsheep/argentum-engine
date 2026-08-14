package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add Emerge [cost] (Eldritch Moon, CR 702.119).
 *
 * "You may cast this spell by paying [cost] and sacrificing a creature rather than paying its mana
 * cost. If you chose to pay this spell's emerge cost, its total cost is reduced by an amount of
 * generic mana equal to the sacrificed creature's mana value."
 *
 * Display-only at the DSL layer — all behavior lives in the engine's alternative-cost pipeline,
 * which keys off the [KeywordAbility.Emerge] entry in `cardDef.keywordAbilities`: the legal-action
 * enumerator offers the cast at the spell's normal timing while a creature the caster controls
 * makes the reduced cost affordable, and the cast handler charges the reduced emerge mana and
 * sacrifices the chosen creature as part of paying the total cost.
 */
fun CardBuilder.emerge(cost: String) {
    keywordAbilityList.add(KeywordAbility.emerge(cost))
}
