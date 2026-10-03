package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Avalanche Caller
 * {1}{U}
 * Snow Creature — Human Wizard
 * 1/3
 * {2}: Target snow land you control becomes a 4/4 Elemental creature with hexproof and haste until
 * end of turn. It's still a land.
 */
val AvalancheCaller = card("Avalanche Caller") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Snow Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "{2}: Target snow land you control becomes a 4/4 Elemental creature with hexproof and haste until end of turn. It's still a land. (A creature with hexproof can't be the target of spells or abilities your opponents control.)"

    activatedAbility {
        cost = Costs.Mana("{2}")
        val land = target(TargetFilter(GameObjectFilter.Land.snow().youControl()))
        effect = Effects.BecomeCreature(
            target = land,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.HEXPROOF, Keyword.HASTE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89cef049-6a47-4264-b2bc-b9d291a09c4c.jpg?1783928269"

        ruling("2021-02-05", "A land that becomes a creature because of Avalanche Caller's activated ability will retain any other supertypes, card types, subtypes, and abilities it had. In particular, it will be a snow creature land.")
        ruling("2021-02-05", "The activated ability doesn't cause the target snow land to become tapped or untapped.")
    }
}
