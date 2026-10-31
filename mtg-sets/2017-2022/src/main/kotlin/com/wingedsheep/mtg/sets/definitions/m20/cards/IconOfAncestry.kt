package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Icon of Ancestry (M20 #229)
 * {3}  Artifact
 *
 * As this artifact enters, choose a creature type.
 * Creatures you control of the chosen type get +1/+1.
 * {3}, {T}: Look at the top three cards of your library. You may reveal a creature card of the
 * chosen type from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order.
 *
 * The Vanquisher's Banner anthem (`chosenSubtypeKey`) plus the look-at-top / reveal-a-match-to-hand
 * shape, filtered by `withChosenSubtype()`.
 */
val IconOfAncestry = card("Icon of Ancestry") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a creature type.\n" +
        "Creatures you control of the chosen type get +1/+1.\n" +
        "{3}, {T}: Look at the top three cards of your library. You may reveal a creature card of the " +
        "chosen type from among them and put it into your hand. Put the rest on the bottom of your " +
        "library in a random order."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures().youControl()
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 3,
            filter = GameObjectFilter.Creature.withChosenSubtype(),
            prompt = "You may reveal a creature card of the chosen type from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b78e53c-1aec-4c49-bf58-165dd7b59d64.jpg"
    }
}
