package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter

/**
 * Dragonspeaker Shaman
 * {1}{R}{R}
 * Creature — Human Barbarian Shaman
 * 2/2
 * Dragon spells you cast cost {2} less to cast.
 */
val DragonspeakerShaman = card("Dragonspeaker Shaman") {
    manaCost = "{1}{R}{R}"
    typeLine = "Creature — Human Barbarian Shaman"
    power = 2
    toughness = 2
    oracleText = "Dragon spells you cast cost {2} less to cast."

    staticAbility {
        ability = ReduceSpellCostByFilter(
            filter = GameObjectFilter.Any.withSubtype("Dragon"),
            amount = 2
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Kev Walker"
        flavorText = "\"We speak the dragons' language of flame and rage. They speak our language of fury and honor. Together we shall weave a tale of destruction without equal.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49f5fa96-dcfb-4d29-bea9-7dd99e8c43d8.jpg?1562528528"
    }
}
