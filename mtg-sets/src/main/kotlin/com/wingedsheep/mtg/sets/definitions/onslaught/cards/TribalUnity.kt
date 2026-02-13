package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeModifyStatsEffect
import com.wingedsheep.sdk.scripting.DynamicAmount

/**
 * Tribal Unity
 * {X}{2}{G}
 * Instant
 * Creatures of the creature type of your choice get +X/+X until end of turn.
 */
val TribalUnity = card("Tribal Unity") {
    manaCost = "{X}{2}{G}"
    typeLine = "Instant"
    oracleText = "Creatures of the creature type of your choice get +X/+X until end of turn."

    spell {
        effect = ChooseCreatureTypeModifyStatsEffect(
            powerModifier = DynamicAmount.XValue,
            toughnessModifier = DynamicAmount.XValue
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "294"
        artist = "Ron Spears"
        flavorText = "\"I may have left the ways of violence behind,\" Kamahl said, \"but I still believe in the power of muscle and bone.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/671d30e4-7c3a-4ffc-8170-6205082bae4e.jpg?1562915955"
    }
}
