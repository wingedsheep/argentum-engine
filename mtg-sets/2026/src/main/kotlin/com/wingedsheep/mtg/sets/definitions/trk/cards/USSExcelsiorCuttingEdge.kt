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
 * U.S.S. Excelsior, Cutting Edge
 * {3}{W}
 * Legendary Artifact — Spacecraft
 * 4/4
 *
 * When U.S.S. Excelsior enters, destroy target tapped creature an opponent controls.
 * Station
 * 8+ | Flying
 */
val USSExcelsiorCuttingEdge = card("U.S.S. Excelsior, Cutting Edge") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 4
    toughness = 4
    oracleText = "When U.S.S. Excelsior enters, destroy target tapped creature an opponent controls.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n" +
        "8+ | Flying"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.TappedCreature.opponentControls())
        effect = Effects.Destroy(creature)
    }

    station()

    val charge8 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)

    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Jetfreak-7"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de62b452-3780-4eeb-8163-f44176495615.jpg?1793100663"
    }
}
