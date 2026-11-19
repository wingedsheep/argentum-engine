package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Griffin Dreamfinder
 * {3}{W}{W}
 * Creature — Griffin
 * 1/4
 *
 * Flying
 * When this creature enters, return target enchantment card from your graveyard to your hand.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val GriffinDreamfinder = card("Griffin Dreamfinder") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    power = 1
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, return target enchantment card from your graveyard to your hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val enchantment = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.ReturnToHand(enchantment)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Adam Paquette"
        flavorText = "Some griffins are able to pluck mystic auras from the Æther that are hidden from other mortals."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b569fadd-d9ca-4b0d-bee2-9d45574e56f0.jpg?1783939579"
    }
}
