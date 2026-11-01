package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Silent Submersible
 * {U}{U}
 * Artifact — Vehicle
 * 2/3
 *
 * Whenever this Vehicle deals combat damage to a player or planeswalker, draw a card.
 * Crew 2
 */
val SilentSubmersible = card("Silent Submersible") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 2
    toughness = 3
    oracleText = "Whenever this Vehicle deals combat damage to a player or planeswalker, draw a card.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        effect = Effects.DrawCards(1)
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae2f3dee-1768-4562-9333-a50b9ee7570f.jpg?1783933458"
    }
}
