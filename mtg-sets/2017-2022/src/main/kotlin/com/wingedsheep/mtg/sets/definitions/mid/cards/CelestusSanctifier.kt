package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Celestus Sanctifier
 * {2}{W}
 * Creature — Human Cleric
 * 3/2
 * If it's neither day nor night, it becomes day as this creature enters.
 * Whenever day becomes night or night becomes day, look at the top two cards of your library.
 * Put one of them into your graveyard.
 *
 * The trigger reuses the Soulcipher Board idiom: [Patterns.Library.lookAtTopAndKeep] with the kept
 * card going to the graveyard and the other left on top of the library.
 */
val CelestusSanctifier = card("Celestus Sanctifier") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 2
    oracleText = "If it's neither day nor night, it becomes day as this creature enters.\n" +
        "Whenever day becomes night or night becomes day, look at the top two cards of your library. " +
        "Put one of them into your graveyard."

    becomesDayAsEntersIfNeither()

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 2,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
        )
        description = "Whenever day becomes night or night becomes day, look at the top two cards of " +
            "your library. Put one of them into your graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67547253-ab98-4d3a-bf95-3e109cf60a85.jpg?1783925663"
    }
}
