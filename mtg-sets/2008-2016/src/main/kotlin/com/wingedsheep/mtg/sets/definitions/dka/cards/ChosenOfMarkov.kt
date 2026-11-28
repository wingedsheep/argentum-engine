package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chosen of Markov // Markov's Servant
 *
 * Front: {2}{B} Creature — Human 2/2.
 * "{T}, Tap an untapped Vampire you control: Transform this creature."
 *
 * The tap-a-Vampire half excludes the source: Chosen of Markov is already tapped by the {T} half of
 * the same cost, so it can't double as the Vampire even if it somehow became one. Only the {T} half
 * is subject to summoning sickness (see ruling).
 *
 * Back: Markov's Servant — Creature — Vampire 4/4, black color indicator.
 */
private val ChosenOfMarkovFront = card("Chosen of Markov") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human"
    power = 2
    toughness = 2
    oracleText = "{T}, Tap an untapped Vampire you control: Transform this creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(
                count = 1,
                filter = GameObjectFilter.Permanent.withSubtype("Vampire").youControl(),
                excludeSelf = true
            )
        )
        effect = Effects.Transform(EffectTarget.Self)
        description = "Transform Chosen of Markov."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Steve Argyle"
        flavorText = "\"I have been found worthy by this great house . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c5a3c09-5656-4975-ba03-2d809903ed18.jpg?1783940840"
        ruling(
            "2011-01-22",
            "You can tap any untapped Vampire you control, including one you haven't controlled " +
                "continuously since the beginning of your most recent turn, to pay the cost of Chosen of " +
                "Markov's activated ability. You must have controlled Chosen of Markov continuously since " +
                "the beginning of your most recent turn, however."
        )
    }
}

private val MarkovsServant = card("Markov's Servant") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Steve Argyle"
        flavorText = "\". . . and for that I shall be eternally grateful.\""
        imageUri = "https://cards.scryfall.io/normal/back/7/c/7c5a3c09-5656-4975-ba03-2d809903ed18.jpg?1783940840"
    }
}

val ChosenOfMarkov: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ChosenOfMarkovFront,
    backFace = MarkovsServant,
)
