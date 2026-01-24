package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.SacrificeUnlessDiscardEffect

/**
 * Pillaging Horde
 * {2}{R}{R}
 * Creature — Human Barbarian
 * 5/5
 * When Pillaging Horde enters the battlefield, sacrifice it unless you discard a card at random.
 */
val PillagingHorde = card("Pillaging Horde") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Human Barbarian"
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = OnEnterBattlefield()
        effect = SacrificeUnlessDiscardEffect(random = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Tom Wänerstrand"
        flavorText = "They take what they want and burn the rest."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cd2e3f4-a5b6-c7d8-e9f0-a1b2c3d4e5f6.jpg"
    }
}
