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
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * U.S.S. Enterprise-A, Free Spirit
 * {3}{W}{W}
 * Legendary Artifact — Spacecraft
 * 5/5
 *
 * Ward {2}
 * When U.S.S. Enterprise-A enters, create three 1/1 red Officer creature tokens.
 * Station
 * 6+ | Flying, lifelink
 */
val USSEnterpriseAFreeSpirit = card("U.S.S. Enterprise-A, Free Spirit") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 5
    toughness = 5
    oracleText = "Ward {2}\n" +
        "When U.S.S. Enterprise-A enters, create three 1/1 red Officer creature tokens.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)\n" +
        "6+ | Flying, lifelink"

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            count = 3,
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
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    staticAbility {
        condition = charge6
        ability = GrantKeyword(Keyword.LIFELINK.name, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "43"
        artist = "Evan Shipard"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99429015-6e5d-4ecf-aaf7-eba181bcffc7.jpg?1792508162"
    }
}
