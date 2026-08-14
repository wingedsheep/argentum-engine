package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add Web-slinging [cost] (Marvel's Spider-Man, CR 702.188).
 *
 * "You may cast this spell by paying [cost] and returning a tapped creature you control to its
 * owner's hand rather than paying its mana cost." (CR 702.188a)
 *
 * Display-only at the DSL layer — all behavior lives in the engine's alternative-cost pipeline,
 * which keys off the [KeywordAbility.WebSlinging] entry in `cardDef.keywordAbilities`: the
 * legal-action enumerator ([com.wingedsheep.engine] `WebSlingingCastEnumerator`) surfaces the cast
 * at the spell's normal timing while the player controls a tapped creature to return, the cast
 * handler charges the web-slinging mana plus returns the chosen tapped creature to hand, and the
 * resolving permanent carries the "web-slinging cost was paid" flag durably (readable via
 * [com.wingedsheep.sdk.dsl.Conditions.WebSlungCostWasPaid]) alongside the returned creature's mana
 * value (read via [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice] on
 * [com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG_RETURNED_MV]).
 */
fun CardBuilder.webSlinging(cost: String) {
    keywordAbilityList.add(KeywordAbility.webSlinging(cost))
}
