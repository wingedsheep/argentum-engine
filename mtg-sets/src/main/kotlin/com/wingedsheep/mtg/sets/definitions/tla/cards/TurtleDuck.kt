package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Turtle-Duck
 * {G}
 * Creature — Turtle Bird
 * 0/4
 * {3}: Until end of turn, this creature has base power 4 and gains trample.
 */
val TurtleDuck = card("Turtle-Duck") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Turtle Bird"
    power = 0
    toughness = 4
    oracleText = "{3}: Until end of turn, this creature has base power 4 and gains trample."

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.Composite(
            Effects.SetBasePower(EffectTarget.Self, DynamicAmount.Fixed(4), Duration.EndOfTurn),
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self, Duration.EndOfTurn),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "200"
        artist = "Sylvain Sarrailh"
        flavorText = "A turtle-duckling's greatest defense comes not from their shell, but from their mother."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aefcd734-3916-4c77-9d98-3ea2c2795658.jpg?1764121358"
    }
}
