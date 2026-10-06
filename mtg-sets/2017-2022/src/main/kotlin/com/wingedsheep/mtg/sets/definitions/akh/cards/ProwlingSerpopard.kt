package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered

/**
 * Prowling Serpopard
 * {1}{G}{G}
 * Creature — Cat Snake
 * 4/3
 * This spell can't be countered.
 * Creature spells you control can't be countered.
 */
val ProwlingSerpopard = card("Prowling Serpopard") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Snake"
    power = 4
    toughness = 3
    oracleText = "This spell can't be countered.\nCreature spells you control can't be countered."

    cantBeCountered = true

    staticAbility {
        ability = GrantCantBeCountered(filter = GameObjectFilter.Creature.youControl())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Tyler Jacobson"
        flavorText = "The viziers serving Rhonas, the god of strength, maintain the menagerie of animals employed during his trial."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92921fc1-11d0-41a9-b9b2-b44fd0913d31.jpg?1783936471"
        ruling(
            "2017-04-18",
            "A spell or ability that counters spells can still target a creature spell you control. When that spell or ability resolves, the creature spell won't be countered, but any additional effects of that spell or ability will still happen."
        )
    }
}
