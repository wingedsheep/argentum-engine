package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Satyr Wayfinder
 * {1}{G}
 * Creature — Satyr
 * 1/1
 *
 * When this creature enters, reveal the top four cards of your library. You may put a land card
 * from among them into your hand. Put the rest into your graveyard.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val SatyrWayfinder = card("Satyr Wayfinder") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, reveal the top four cards of your library. You may put a land card " +
        "from among them into your hand. Put the rest into your graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(4),
            filter = GameObjectFilter.Land,
            prompt = "You may put a land card from among them into your hand",
            revealed = true,
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Steve Prescott"
        flavorText = "The first satyr to wake after a revel must search for the site of the next one."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13c5a1ce-932a-4b3d-8b86-ed920e646afc.jpg?1783939529"
    }
}
