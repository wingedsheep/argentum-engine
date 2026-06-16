package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Goldvein Hydra
 * {X}{G}
 * Creature — Hydra
 * 0/0
 * Vigilance, trample, haste
 * This creature enters with X +1/+1 counters on it.
 * When this creature dies, create a number of tapped Treasure tokens equal to its power.
 */
val GoldveinHydra = card("Goldvein Hydra") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "Vigilance, trample, haste\nThis creature enters with X +1/+1 counters on it.\nWhen this creature dies, create a number of tapped Treasure tokens equal to its power."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.HASTE)

    // Enters with X +1/+1 counters
    replacementEffect(EntersWithDynamicCounters(count = DynamicAmount.XValue))

    // When this creature dies, create tapped Treasure tokens equal to its (last-known) power
    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateTreasure(count = DynamicAmounts.sourcePower(), tapped = true)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "167"
        artist = "David Auden Nash"
        flavorText = "The key is to slay it before it sprouts so many heads that your fear outpaces your greed."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de0e01e4-e143-47fd-8565-7b48219bb546.jpg?1712355937"
    }
}
