package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * U.S.S. Enterprise-D, Galaxy-Class
 * {3}
 * Legendary Artifact — Spacecraft
 * 4/5
 *
 * Whenever one or more charge counters are put on U.S.S. Enterprise-D for the first time each turn,
 * exile the top card of your library. You may play that card this turn.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)
 * 7+ | Flying, vigilance
 */
val USSEnterpriseDGalaxyClass = card("U.S.S. Enterprise-D, Galaxy-Class") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Spacecraft"
    power = 4
    toughness = 5
    oracleText = "Whenever one or more charge counters are put on U.S.S. Enterprise-D for the first time " +
        "each turn, exile the top card of your library. You may play that card this turn.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)\n" +
        "7+ | Flying, vigilance"

    triggeredAbility {
        trigger = Triggers.self.getsCounters(type = CounterType.CHARGE, firstTimeEachTurn = true)
        effect = Patterns.Exile.impulse(1)
        description = "Whenever one or more charge counters are put on U.S.S. Enterprise-D for the first " +
            "time each turn, exile the top card of your library. You may play that card this turn."
    }

    station()

    val charge7 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 7)

    staticAbility {
        condition = charge7
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge7
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    staticAbility {
        condition = charge7
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "273"
        artist = "Pablo Mendoza"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/057a4413-6a17-491e-bfd7-6cd427b1a442.jpg?1784078038"
    }
}
