package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Archaeomancer
 * {2}{U}{U}
 * Creature — Human Wizard
 * 1/2
 * When this creature enters, return target instant or sorcery card from your graveyard to your hand.
 */
val Archaeomancer = card("Archaeomancer") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, return target instant or sorcery card from your graveyard to your hand."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.InstantOrSorceryInGraveyard.ownedByYou())
        effect = Effects.Move(
            target = t,
            destination = Zone.HAND
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Zoltan Boros"
        flavorText = "\"Words of power never disappear. They sleep, awaiting those with the will to rouse them.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73c6d1be-55ad-4ee4-b044-88438e9b78cc.jpg?1783940513"
    }
}
