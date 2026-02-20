package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Spiritual Guardian
 * {3}{W}{W}
 * Creature - Spirit
 * 3/4
 * When Spiritual Guardian enters the battlefield, you gain 4 life.
 */
val SpiritualGuardian = card("Spiritual Guardian") {
    manaCost = "{3}{W}{W}"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = GainLifeEffect(4)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Terese Nielsen"
        flavorText = "\"Hope is born within.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dbea02f-9124-4e1a-8693-d988a0a3adae.jpg"
    }
}
