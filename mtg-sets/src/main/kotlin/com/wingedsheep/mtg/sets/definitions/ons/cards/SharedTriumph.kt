package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Shared Triumph
 * {1}{W}
 * Enchantment
 * As Shared Triumph enters the battlefield, choose a creature type.
 * Creatures of the chosen type get +1/+1.
 */
val SharedTriumph = card("Shared Triumph") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment"
    oracleText = "As Shared Triumph enters the battlefield, choose a creature type.\nCreatures of the chosen type get +1/+1."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Mark Brill"
        flavorText = "\"Win together, die alone.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d07ebe6-76cf-4345-b59b-9954496c44d0.jpg?1562898003"
    }
}
