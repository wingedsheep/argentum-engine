package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Garrison Sergeant
 * {3}{R}{W}
 * Creature — Lizard Soldier
 * 3/3
 *
 * This creature has double strike as long as you control a Gate.
 */
val GarrisonSergeant = card("Garrison Sergeant") {
    manaCost = "{3}{R}{W}"
    typeLine = "Creature — Lizard Soldier"
    power = 3
    toughness = 3
    oracleText = "This creature has double strike as long as you control a Gate."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.Self),
            condition = Conditions.ControlPermanentOfType(Subtype.GATE),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Gabor Szikszai"
        flavorText = "In the Legion, no flagpole is merely decorative, and every ceremonial sword bears an edge."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bd957a1-a354-48e9-80ad-66e49aa845fc.jpg?1783934134"
    }
}
