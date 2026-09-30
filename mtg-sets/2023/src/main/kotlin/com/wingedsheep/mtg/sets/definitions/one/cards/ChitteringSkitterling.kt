package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Chittering Skitterling
 * {2}{B}
 * Creature — Phyrexian Rat
 * 1/4
 * Corrupted — Sacrifice an artifact or creature: Draw a card. Activate only if an opponent has
 * three or more poison counters and only once each turn.
 */
val ChitteringSkitterling = card("Chittering Skitterling") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Rat"
    oracleText = "Corrupted — Sacrifice an artifact or creature: Draw a card. Activate only if an opponent has three or more poison counters and only once each turn."
    power = 1
    toughness = 4

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Artifact or GameObjectFilter.Creature)
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.Corrupted),
            ActivationRestriction.OncePerTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Nils Hamm"
        flavorText = "What cannot be remade will be unmade, one scrap at a time."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0328d43-ae9b-462a-a1e5-8ed408eea1a7.jpg?1783918050"
    }
}
