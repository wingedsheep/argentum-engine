package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fourth Bridge Prowler
 * {B}
 * Creature — Human Rogue
 * 1/1
 * When this creature enters, you may have target creature get -1/-1 until end of turn.
 */
val FourthBridgeProwler = card("Fourth Bridge Prowler") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, you may have target creature get -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-1, -1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "John Silva"
        flavorText = "Renegades run the gamut from lofty sages to petty thieves."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73aaaa09-c985-42f8-b426-06fd3b8de66d.jpg?1783936763"
        ruling("2017-02-09", "Fourth Bridge Prowler can be the target of its own ability.")
    }
}
