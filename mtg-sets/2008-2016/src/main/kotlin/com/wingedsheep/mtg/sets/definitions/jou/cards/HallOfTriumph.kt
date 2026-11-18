package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hall of Triumph
 * {3}
 * Legendary Artifact
 * As Hall of Triumph enters, choose a color.
 * Creatures you control of the chosen color get +1/+1.
 *
 * The chosen color is stored via [EntersWithChoice]`(ChoiceType.COLOR)`; the lord filter
 * (`withChosenColor()`) reads it from the source.
 */
val HallOfTriumph = card("Hall of Triumph") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "As Hall of Triumph enters, choose a color.\n" +
        "Creatures you control of the chosen color get +1/+1."

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withChosenColor())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Ryan Yee"
        flavorText = "Heroes act without thought of glory or reward. Accolades are a consequence, not a goal."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/634fd988-5d5c-4637-b7fd-05a41798ead8.jpg?1783939399"
    }
}
