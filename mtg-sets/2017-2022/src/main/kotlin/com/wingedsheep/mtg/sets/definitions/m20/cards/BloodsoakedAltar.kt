package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Bloodsoaked Altar (M20 #90)
 * {4}{B}{B}  Artifact
 *
 * {T}, Pay 2 life, Discard a card, Sacrifice a creature: Create a 5/5 black Demon creature token
 * with flying. Activate only as a sorcery.
 */
val BloodsoakedAltar = card("Bloodsoaked Altar") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{T}, Pay 2 life, Discard a card, Sacrifice a creature: Create a 5/5 black Demon creature token with flying. " +
        "Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.PayLife(2),
            Costs.DiscardCard,
            Costs.Sacrifice(GameObjectFilter.Creature)
        )
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Demon"),
            keywords = setOf(Keyword.FLYING)
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Sidharth Chaturvedi"
        flavorText = "Blood seeps into the stone, and darkness emerges."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9169431-a5b1-42ea-bcf4-6c750d2a2dbb.jpg"
    }
}
