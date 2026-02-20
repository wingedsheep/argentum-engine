package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Aura Extraction
 * {1}{W}
 * Instant
 * Put target enchantment on top of its owner's library.
 * Cycling {2}
 */
val AuraExtraction = card("Aura Extraction") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Put target enchantment on top of its owner's library.\nCycling {2}"

    spell {
        target = Targets.Enchantment
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.LIBRARY, ZonePlacement.Top)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Luca Zontini"
        flavorText = "Every day, Order clerics contain as much of the Mirari's energy as possible, hoping to delay Otaria's demise."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55d16883-5e98-4dd2-92dd-0ba92f1099cb.jpg?1562915090"
    }
}
