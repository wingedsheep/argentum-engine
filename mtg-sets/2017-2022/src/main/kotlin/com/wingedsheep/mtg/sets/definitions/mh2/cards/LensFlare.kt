package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Lens Flare — Modern Horizons 2 #20
 * {4}{W} · Instant
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * Lens Flare deals 5 damage to target attacking or blocking creature.
 *
 * Pure composition, the Piercing Light / Divine Arrow combat-trick shape with an affinity rider.
 * [KeywordAbility.Affinity] for [CardType.ARTIFACT] is engine-live vocabulary — the cost calculator
 * reads the *KeywordAbility*, never a `Keyword.AFFINITY` enum entry, so the display keyword is left
 * for `CardBuilder.build()` to derive. Affinity only shaves the generic portion of the cost, so five
 * artifacts floor this at {W}: the coloured pip is never reduced away and the mana value stays 5.
 *
 * The combat restriction is [TargetFilter.AttackingOrBlockingCreature] rather than two separate
 * target slots — it is one creature filter carrying a state-level "attacking or blocking"
 * disjunction, which is evaluated against projected state like every other battlefield filter.
 */
val LensFlare = card("Lens Flare") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Lens Flare deals 5 damage to target attacking or blocking creature."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.AttackingOrBlockingCreature))
        effect = Effects.DealDamage(5, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Josh Hass"
        flavorText = "The key for surviving a war is never becoming its focus."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d92f037-a121-428a-ac53-98437366ecfd.jpg?1783926889"
    }
}
