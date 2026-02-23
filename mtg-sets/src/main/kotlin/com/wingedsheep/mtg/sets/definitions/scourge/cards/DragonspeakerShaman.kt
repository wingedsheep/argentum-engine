package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype

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
        ability = ReduceSpellCostBySubtype(
            subtype = "Dragon",
            amount = 2
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Kev Walker"
        flavorText = "We speak the dragons' language of flame and rage. They speak our language of fury and honor. Together we shall weave a tale of destruction without equal."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfeb1145-3729-481e-a314-c325ed2f2a35.jpg?1562857517"
    }
}
