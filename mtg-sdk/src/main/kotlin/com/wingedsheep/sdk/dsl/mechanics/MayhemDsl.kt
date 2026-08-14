package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add Mayhem [cost] (Marvel's Spider-Man, CR 702.187).
 *
 * "As long as you discarded this card this turn, you may cast it from your graveyard by paying
 * [cost] rather than paying its mana cost." (CR 702.187b)
 *
 * Display-only at the DSL layer — all behavior lives in the engine's alternative-cost pipeline,
 * which keys off the [KeywordAbility.Mayhem] entry in `cardDef.keywordAbilities`: the graveyard
 * legal-action enumerator (`CastFromZoneEnumerator.enumerateMayhem`) surfaces the cast at the
 * spell's normal timing, gated on the "you discarded this card this turn" tracker
 * ([com.wingedsheep.sdk.dsl.Conditions.YouDiscardedThisCardThisTurn]); the cast handler charges the
 * Mayhem mana instead of the mana cost; and — unlike Flashback/Harmonize — the spell is NOT exiled
 * on resolution (a permanent just enters the battlefield, an instant/sorcery goes to the graveyard
 * as normal). A resolving spell carries the "mayhem cost was paid" flag durably (readable via
 * [com.wingedsheep.sdk.dsl.Conditions.MayhemCostWasPaid]) for payoffs like Sandman's Quicksand.
 *
 * Pass "" for the CR 702.187c "Mayhem" (no cost) land form (Oscorp Industries).
 */
fun CardBuilder.mayhem(cost: String) {
    keywordAbilityList.add(KeywordAbility.mayhem(cost))
}
