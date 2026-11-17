package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Obelisk of Urd
 * {6}
 * Artifact
 * Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)
 * As this artifact enters, choose a creature type.
 * Creatures you control of the chosen type get +2/+2.
 */
val ObeliskOfUrd = card("Obelisk of Urd") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "As this artifact enters, choose a creature type.\n" +
        "Creatures you control of the chosen type get +2/+2."

    keywords(Keyword.CONVOKE)

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter.ChosenSubtypeCreatures().youControl()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "John Severin Brassell"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed7865a7-8fe2-4b5d-a625-91585d1b7d4a.jpg"
    }
}
