package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.withoutSubtypeFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.OptionType

/**
 * Kindred Dominance — Commander 2017 #18 (canonical printing)
 * {5}{B}{B} · Sorcery
 *
 * Choose a creature type. Destroy all creatures that aren't of the chosen type.
 *
 * Kindred Judgment's shape: the type is chosen on resolution, and the destroy set reads projected
 * subtypes, so a changeling (every creature type) is always spared.
 */
val KindredDominance = card("Kindred Dominance") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a creature type. Destroy all creatures that aren't of the chosen type."

    spell {
        effect = Effects.Pipeline {
            val chosenType = chooseOption(OptionType.CREATURE_TYPE)
            run(Effects.DestroyAll(GameObjectFilter.Creature.withoutSubtypeFromVariable(chosenType)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Bram Sels"
        flavorText = "In the end, the lights fade, the land withers, and only the dead and vile remain."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9794115a-5509-4d9a-b119-d2b61942e87b.jpg?1783935945"
        ruling("2017-08-25", "Once Kindred Dominance begins to resolve, no players may take other actions until it's done. Notably, players can't try to save their creatures after you've chosen a creature type.")
        ruling("2017-08-25", "You can't choose multiple creature types, such as \"Cat Warrior.\" A Cat Warrior is both a Cat and a Warrior. It's affected by anything that affects either type and unaffected by things that affect non-Cat or non-Warrior creatures.")
    }
}
