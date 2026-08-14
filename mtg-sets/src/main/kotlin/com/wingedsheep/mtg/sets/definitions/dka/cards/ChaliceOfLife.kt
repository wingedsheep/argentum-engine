package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val ChaliceOfLifeFront = card("Chalice of Life") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: You gain 1 life. Then if you have at least 10 life more than your " +
        "starting life total, transform this artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            Effects.GainLife(1),
            ConditionalEffect(
                condition = Conditions.LifeAboveStartingBy(10),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "Gain 1 life, then transform Chalice of Life if you have at least 10 life " +
            "more than your starting life total."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Ryan Yee"
        flavorText = "The sweet taste of hope's promise."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d9c1c46-7aa7-464c-87b0-b29b9663daef.jpg?1783940798"
        ruling("2011-01-22", "Your starting life total is the life total you began the game with.")
        ruling("2011-01-22", "You check your life total to see if Chalice of Life transforms only when its ability resolves.")
    }
}

private val ChaliceOfDeath = card("Chalice of Death") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Target player loses 5 life."

    activatedAbility {
        cost = Costs.Tap
        val player = target("target player", Targets.Player)
        effect = Effects.LoseLife(5, player)
        description = "Target player loses 5 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Ryan Yee"
        flavorText = "The bitter taste of life's only certainty."
        imageUri = "https://cards.scryfall.io/normal/back/9/d/9d9c1c46-7aa7-464c-87b0-b29b9663daef.jpg?1783940798"
    }
}

val ChaliceOfLife: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ChaliceOfLifeFront,
    backFace = ChaliceOfDeath,
)
