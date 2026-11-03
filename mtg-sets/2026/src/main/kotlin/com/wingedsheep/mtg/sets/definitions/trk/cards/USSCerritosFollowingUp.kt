package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
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
 * U.S.S. Cerritos, Following Up
 * {5}
 * Legendary Artifact — Spacecraft
 * 4/4
 *
 * When U.S.S. Cerritos enters, create two 1/1 red Officer creature tokens.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)
 * 6+ | Flying
 */
val USSCerritosFollowingUp = card("U.S.S. Cerritos, Following Up") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Spacecraft"
    power = 4
    toughness = 4
    oracleText = "When U.S.S. Cerritos enters, create two 1/1 red Officer creature tokens.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)\n" +
        "6+ | Flying"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "272"
        artist = "Bruce Brenneise"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76bf7521-5024-4fdc-9ea1-5e10f2647efb.jpg?1793361418"
    }
}
