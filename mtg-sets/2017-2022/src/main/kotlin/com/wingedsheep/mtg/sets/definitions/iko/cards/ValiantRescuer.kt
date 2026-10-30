package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Valiant Rescuer
 * {1}{W}
 * Creature — Human Soldier
 * 3/1
 * Whenever you cycle another card for the first time each turn, create a 1/1 white Human Soldier
 * creature token.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 *
 * "Another" needs no filter (see Drannith Stinger): the observer only functions on the
 * battlefield and a card is cycled from hand.
 */
val ValiantRescuer = card("Valiant Rescuer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 1
    oracleText = "Whenever you cycle another card for the first time each turn, create a 1/1 white Human Soldier creature token.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.you.cycles(firstTimeEachTurn = true)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier")
        )
        description = "Whenever you cycle another card for the first time each turn, create a 1/1 white Human Soldier creature token."
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Bram Sels"
        flavorText = "The floating city of Skysail has two rules: don't leave your crewmates behind, and don't mess with the guardrails."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2b13ff6-67e9-4760-aef4-a8548ea44344.jpg?1783931081"
    }
}
