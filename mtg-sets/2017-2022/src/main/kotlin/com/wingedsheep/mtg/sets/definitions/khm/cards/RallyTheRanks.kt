package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Rally the Ranks
 * {1}{W}
 * Enchantment
 * As this enchantment enters, choose a creature type.
 * Creatures you control of the chosen type get +1/+1.
 */
val RallyTheRanks = card("Rally the Ranks") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a creature type.\n" +
        "Creatures you control of the chosen type get +1/+1."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures().youControl()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Lie Setiawan"
        flavorText = "Halvar united the spirits of all the clans into a single, devastating force."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03595195-3be2-4d18-b5c0-43b2dcc1c0f5.jpg?1783928282"
    }
}
