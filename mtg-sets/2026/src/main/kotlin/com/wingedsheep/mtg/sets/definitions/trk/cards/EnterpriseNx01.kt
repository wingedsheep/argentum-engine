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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enterprise NX-01
 * {3}{U}
 * Legendary Artifact — Spacecraft
 * 4/3
 *
 * When Enterprise NX-01 enters, tap target creature an opponent controls and put a stun counter on it.
 * (If a permanent with a stun counter would become untapped, remove one from it instead.)
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)
 * 6+ | Flying, vigilance
 */
val EnterpriseNx01 = card("Enterprise NX-01") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 4
    toughness = 3
    oracleText = "When Enterprise NX-01 enters, tap target creature an opponent controls and put a stun " +
        "counter on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)\n" +
        "6+ | Flying, vigilance"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.opponentControls())
        effect = Effects.Tap(creature) then Effects.AddCounters(CounterType.STUN, 1, creature)
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

    staticAbility {
        condition = charge6
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Colin Searle"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/385638fe-cafb-4541-ba37-19bc57520c48.jpg?1793100690"
    }
}
