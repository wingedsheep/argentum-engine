package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureGroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForGroupEffect

/**
 * Steadfastness
 * {1}{W}
 * Sorcery
 * Creatures you control get +0/+3 until end of turn.
 */
val Steadfastness = card("Steadfastness") {
    manaCost = "{1}{W}"
    typeLine = "Sorcery"

    spell {
        effect = ModifyStatsForGroupEffect(
            powerModifier = 0,
            toughnessModifier = 3,
            filter = CreatureGroupFilter.AllYouControl
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Kev Walker"
        flavorText = "\"Brute force wins the battles. Conviction wins the wars.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb4693b3-d5d7-4401-aee3-119d3eb276a2.jpg"
    }
}
