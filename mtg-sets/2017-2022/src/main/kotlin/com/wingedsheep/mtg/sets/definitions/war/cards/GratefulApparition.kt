package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Grateful Apparition
 * {1}{W}
 * Creature — Spirit
 * 1/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player or planeswalker, proliferate.
 */
val GratefulApparition = card("Grateful Apparition") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player or planeswalker, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dcb1f9d-105d-48c6-bd66-d7e8839de9e1.jpg?1783933482"
        ruling("2023-02-04", "You don't have to choose every permanent or player that has a counter, only the ones you want to add another counter to. Since \"any number\" includes zero, you don't have to choose any permanents at all, and you don't have to choose any players at all.")
        ruling("2023-02-04", "If a player or permanent has more than one kind of counter on it, and you choose for it to get additional counters, it must get one of each kind of counter it already has. You can't have it get just one kind of counter it already has and not the others.")
    }
}
