package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * White Lotus Tile
 * {4}
 * Artifact
 *
 * This artifact enters tapped.
 * {T}: Add X mana of any one color, where X is the greatest number of creatures you control that
 * have a creature type in common.
 *
 * "Add X mana of any one color" is exactly [AddManaOfChoiceEffect] over [ManaColorSet.AnyColor]
 * with a dynamic amount — the player picks one color and gets X copies of it. X is the new
 * [DynamicAmount.LargestSharedCreatureTypeCount] (largest creature-type tribe among your
 * creatures, multi-type creatures and Changelings counting toward each tribe).
 */
val WhiteLotusTile = card("White Lotus Tile") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{T}: Add X mana of any one color, where X is the greatest number of creatures you control " +
        "that have a creature type in common."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = AddManaOfChoiceEffect(
            colorSet = ManaColorSet.AnyColor,
            amount = DynamicAmount.LargestSharedCreatureTypeCount(Player.You),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "262"
        artist = "Dee Nguyen"
        flavorText = "\"Most people think the lotus tile insignificant, but it is essential for the unusual strategy that I employ.\"\n—Iroh"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d70f9ec-fdc1-4219-b89b-c030d712c1fc.jpg?1764121931"
    }
}
