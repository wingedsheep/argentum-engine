package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add Madness [cost] (CR 702.35).
 *
 * "If you discard this card, discard it into exile. When you do, cast it for its madness cost or
 * put it into your graveyard."
 *
 * Display-only at the DSL layer — both halves of the keyword live in the engine and key off the
 * [KeywordAbility.Madness] entry in `cardDef.keywordAbilities`: the discard replacement redirects
 * the card to exile from every discard path (CR 702.35a), and the exile stamps the marker that
 * makes the engine put [com.wingedsheep.sdk.scripting.Madness.castAbility] on the stack. Declining
 * that cast — or being unable to pay for it — puts the card into its owner's graveyard.
 */
fun CardBuilder.madness(cost: String) {
    keywordAbilityList.add(KeywordAbility.madness(cost))
}
