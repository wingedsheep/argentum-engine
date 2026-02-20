package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageFromChosenCreatureTypeEffect

/**
 * Circle of Solace
 * {3}{W}
 * Enchantment
 * As Circle of Solace enters the battlefield, choose a creature type.
 * {1}{W}: The next time a creature of the chosen type would deal damage to you this turn, prevent that damage.
 */
val CircleOfSolace = card("Circle of Solace") {
    manaCost = "{3}{W}"
    typeLine = "Enchantment"
    oracleText = "As Circle of Solace enters the battlefield, choose a creature type.\n{1}{W}: The next time a creature of the chosen type would deal damage to you this turn, prevent that damage."

    replacementEffect(EntersWithCreatureTypeChoice())

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = PreventNextDamageFromChosenCreatureTypeEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/large/front/0/7/07f567dc-8a60-40e1-b947-199872d8df08.jpg?1562896941"
    }
}
