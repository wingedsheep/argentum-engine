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
 * U.S.S. Discovery, at Black Alert
 * {3}{G}{G}
 * Legendary Artifact — Spacecraft
 * 5/5
 *
 * Whenever U.S.S. Discovery enters or attacks, discover 5.
 * Station
 * 6+ | Flying
 *
 * - The "enters or attacks" trigger is two triggered abilities sharing one effect — the two events
 *   can never coincide, so splitting them is rules-identical.
 * - Its station reminder is omitted on the card, but like every Spacecraft with a power/toughness
 *   box it's an artifact creature at the threshold that box sits on (6+), as Borg Cube is at 12+.
 */
val USSDiscoveryAtBlackAlert = card("U.S.S. Discovery, at Black Alert") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 5
    toughness = 5
    oracleText = "Whenever U.S.S. Discovery enters or attacks, discover 5. (Exile cards from the top of " +
        "your library until you exile a nonland card with mana value 5 or less. Cast it without paying " +
        "its mana cost or put it into your hand. Put the rest on the bottom in a random order.)\n" +
        "Station\n" +
        "6+ | Flying"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Discover(5)
        description = "Whenever U.S.S. Discovery enters, discover 5."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Discover(5)
        description = "Whenever U.S.S. Discovery attacks, discover 5."
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
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Kevin Glint"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69b1f87f-ff02-4945-9f07-18dab5035e2d.jpg?1792508275"
    }
}
