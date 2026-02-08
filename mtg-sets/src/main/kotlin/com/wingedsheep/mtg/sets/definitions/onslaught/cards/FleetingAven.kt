package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.OnCycle

/**
 * Fleeting Aven
 * {1}{U}{U}
 * Creature — Bird Wizard
 * 2/2
 * Flying
 * Whenever a player cycles a card, return Fleeting Aven to its owner's hand.
 */
val FleetingAven = card("Fleeting Aven") {
    manaCost = "{1}{U}{U}"
    typeLine = "Creature — Bird Wizard"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = OnCycle(controllerOnly = false)
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Gary Ruddell"
        flavorText = "\"Don't become so enthralled with magic that you forget you can fly without it.\"\n—Mystic elder"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/246a2758-0096-43b9-8193-d6ae5b41b6e6.jpg"
    }
}
