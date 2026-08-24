package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rushwood Elemental
 * {G}{G}{G}{G}{G}
 * Creature — Elemental
 * 4 / 4
 */
val RushwoodElemental = card("Rushwood Elemental") {
    manaCost = "{G}{G}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "Trample\n" +
        "At the beginning of your upkeep, you may put a +1/+1 counter on this creature."
    power = 4
    toughness = 4

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "264"
        artist = "Hannibal King"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52128694-d9f5-4acb-b684-bb02a4e766b8.jpg"
    }
}
