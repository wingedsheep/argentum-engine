package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Class-F Shuttlecraft
 * {3}
 * Artifact — Spacecraft
 * 2/4
 *
 * {T}: Add one mana of any color.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)
 * 6+ | Flying, vigilance
 */
val ClassFShuttlecraft = card("Class-F Shuttlecraft") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Spacecraft"
    power = 2
    toughness = 4
    oracleText = "{T}: Add one mana of any color.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 6+.)\n" +
        "6+ | Flying, vigilance"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
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
        rarity = Rarity.COMMON
        collectorNumber = "259"
        artist = "William Phifer"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55e0a183-4145-472f-8bed-a1f247febe06.jpg?1793361394"
    }
}
