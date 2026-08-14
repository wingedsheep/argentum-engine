package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add Disturb [cost] (Innistrad: Midnight Hunt / Crimson Vow, CR 702.146).
 *
 * "You may cast this card transformed from your graveyard by paying [cost] rather than its mana
 * cost." (CR 702.146a) Belongs on the **front** face of a transforming double-faced card whose back
 * face is a permanent — that back face is what the disturb cast puts on the stack.
 *
 * Display-only at the DSL layer — all behavior lives in the engine's alternative-cost pipeline,
 * which keys off the [KeywordAbility.Disturb] entry in `cardDef.keywordAbilities`. The graveyard
 * legal-action enumerator (`CastFromZoneEnumerator.enumerateDisturb`) surfaces the cast at the *back
 * face's* normal timing and with the back face's targets (an Aura back chooses what it enchants as
 * it is cast), the cast handler charges the disturb mana instead of the mana cost and flips the card
 * to its back face before it becomes a spell, and — per CR 712.8c — the resulting spell has only the
 * back face's characteristics while its mana value still comes from the front face's mana cost.
 *
 * Unlike flashback/harmonize, a disturb cast does not exile the card on resolution. Every printed
 * disturb back face carries its own "if this would be put into a graveyard from anywhere, exile it
 * instead" replacement instead, which is what stops it being disturbed twice.
 */
fun CardBuilder.disturb(cost: String) {
    keywordAbilityList.add(KeywordAbility.disturb(cost))
}
