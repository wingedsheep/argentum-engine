package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Suurok-Class Science Vessel
 * {2}{U}
 * Artifact — Spacecraft
 * 3/2
 *
 * When this Spacecraft enters, draw a card.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 5+.)
 * 5+ | Flying
 */
val SuurokClassScienceVessel = card("Suurok-Class Science Vessel") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Spacecraft"
    power = 3
    toughness = 2
    oracleText = "When this Spacecraft enters, draw a card.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 5+.)\n" +
        "5+ | Flying"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    station()

    val charge5 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 5)

    staticAbility {
        condition = charge5
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge5
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Pace Wilder"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7608943e-329e-41f9-b098-a0a94b01e6a1.jpg?1793361260"
    }
}
