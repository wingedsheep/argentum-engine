package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Glidedive Duo
 * {4}{B}
 * Creature — Bat Lizard
 * 3/3
 *
 * Flying
 * When this creature enters, each opponent loses 2 life and you gain 2 life.
 */
val GlidediveDuo = card("Glidedive Duo") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat Lizard"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhen this creature enters, each opponent loses 2 life and you gain 2 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                LoseLifeEffect(DynamicAmount.Fixed(2), EffectTarget.PlayerRef(Player.EachOpponent)),
                GainLifeEffect(DynamicAmount.Fixed(2), EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Manuel Castañón"
        flavorText = "\"Catch the thermal!\" called the bat.\n\"What's a thermal?!\" cried the lizard."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4831e7ae-54e3-4bd9-b5af-52dc29f81715.jpg?1721426425"
    }
}
