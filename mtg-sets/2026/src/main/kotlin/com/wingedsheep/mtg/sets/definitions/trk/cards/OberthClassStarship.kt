package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Oberth-Class Starship
 * {1}{W}
 * Artifact — Spacecraft
 * 2/2
 *
 * When this Spacecraft enters, search your library for a basic Plains card, reveal it, put it into
 * your hand, then shuffle.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)
 * 6+ | Flying
 */
val OberthClassStarship = card("Oberth-Class Starship") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Spacecraft"
    power = 2
    toughness = 2
    oracleText = "When this Spacecraft enters, search your library for a basic Plains card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)\n" +
        "6+ | Flying"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    station()

    val charge6 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 6)

    staticAbility {
        condition = charge6
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge6
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Jessica Rossier"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7efe3788-ecf7-4b56-8970-bc324092941c.jpg?1793361187"
    }
}
