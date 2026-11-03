package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * U.S.S. Voyager, Lone Wanderer
 * {3}{G}
 * Legendary Artifact — Spacecraft
 * 3/3
 *
 * When U.S.S. Voyager enters, reveal cards from the top of your library until you reveal two land
 * cards. Put those land cards onto the battlefield tapped and the rest on the bottom of your library
 * in a random order.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)
 * 8+ | Flying, vigilance
 *
 * The reveal walk stops at the second land; every land revealed (both, or the lone one if the
 * library runs out first) goes onto the battlefield tapped, the rest to the bottom in random order.
 */
val USSVoyagerLoneWanderer = card("U.S.S. Voyager, Lone Wanderer") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 3
    toughness = 3
    oracleText = "When U.S.S. Voyager enters, reveal cards from the top of your library until you reveal " +
        "two land cards. Put those land cards onto the battlefield tapped and the rest on the bottom of " +
        "your library in a random order.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n" +
        "8+ | Flying, vigilance"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline(
            descriptionOverride = "Reveal cards from the top of your library until you reveal two land " +
                "cards. Put those land cards onto the battlefield tapped and the rest on the bottom of " +
                "your library in a random order."
        ) {
            val (_, allRevealed) = gatherUntilMatch(GameObjectFilter.Land, count = DynamicAmounts.fixed(2))
            reveal(allRevealed)
            val (lands, rest) = filterSplit(allRevealed, GameObjectFilter.Land)
            move(lands, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    station()

    val charge8 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)

    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Hector Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaf8a0b2-c6cb-41cb-913d-7ae1d75e555d.jpg?1793274284"
    }
}
