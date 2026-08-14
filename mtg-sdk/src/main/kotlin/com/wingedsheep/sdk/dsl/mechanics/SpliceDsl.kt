package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add Splice onto [onto] [cost] (CR 702.47).
 *
 * "You may reveal this card from your hand as you cast a [onto] spell. If you do, that spell gains
 * the text of this card's rules text and you pay [cost] as an additional cost to cast that spell."
 *
 * Display-only at the DSL layer — the card's own `spellEffect` and `targetRequirements` are the very
 * text that gets spliced, so a splice card needs nothing beyond this one line plus the normal spell
 * script it would use when cast on its own. The engine's cast pipeline surfaces a `CastWithSplice`
 * variant per splice card in hand whenever the caster casts a spell carrying [onto], charges [cost]
 * as an additional cost (CR 601.2b / 601.2f–h), reveals the card without moving it out of hand, and
 * appends its effect after the main spell's own effects when the spell resolves (CR 702.47b).
 */
fun CardBuilder.splice(cost: String, onto: Subtype = Subtype.ARCANE) {
    keywordAbilityList.add(KeywordAbility.splice(cost, onto))
}

/**
 * This card's splice ability (CR 702.47), or null when it has none. The engine's cast pipeline reads
 * it to decide whether a card in hand can be revealed and spliced onto the spell being cast.
 */
fun CardDefinition.spliceKeyword(): KeywordAbility.Splice? =
    keywordAbilities.filterIsInstance<KeywordAbility.Splice>().firstOrNull()
