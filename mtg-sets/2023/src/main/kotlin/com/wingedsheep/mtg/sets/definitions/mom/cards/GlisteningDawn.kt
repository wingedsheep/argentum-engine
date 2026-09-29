package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Glistening Dawn — March of the Machine #187
 * {2}{G}{G} · Sorcery
 *
 * Incubate X twice, where X is the number of lands you control.
 *
 * Two separate Incubator tokens, each with X +1/+1 counters; X is read on resolution.
 */
val GlisteningDawn = card("Glistening Dawn") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Incubate X twice, where X is the number of lands you control. (To incubate X, create an " +
        "Incubator token with X +1/+1 counters on it and \"{2}: Transform this token.\" It transforms into " +
        "a 0/0 Phyrexian artifact creature.)"

    spell {
        effect = Effects.Incubate(DynamicAmounts.landsYouControl()) then
            Effects.Incubate(DynamicAmounts.landsYouControl())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Chris Ostrowski"
        flavorText = "Unleashed on new worlds, the Phyrexians were driven by a simple imperative: spread and conquer."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/5443f4f6-a549-4912-a919-69e3570a9933.jpg?1783916968"
        ruling(
            "2023-04-14",
            "Use the number of lands you control as Glistening Dawn resolves to determine the value of X."
        )
    }
}
