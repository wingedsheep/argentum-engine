package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Foolish Fate
 * {2}{B}
 * Instant
 * Destroy target creature.
 * Infusion — If you gained life this turn, that creature's controller loses 3 life.
 *
 * Infusion is an ability word (no rules meaning); it flavors the intervening-if clause.
 * "That creature's controller" refers to the controller of the targeted creature, captured
 * via [EffectTarget.TargetController] so the drain still resolves against the right player
 * even though the creature itself has left the battlefield.
 */
val FoolishFate = card("Foolish Fate") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature.\n" +
        "Infusion — If you gained life this turn, that creature's controller loses 3 life."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Destroy(creature) then ConditionalEffect(
            condition = Conditions.YouGainedLifeThisTurn,
            effect = Effects.LoseLife(3, EffectTarget.TargetController),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Danny Schwartz"
        flavorText = "\"There are some lessons so simple that I thought they needn't be taught. " +
            "I suppose I was wrong.\"\n—Moseo, dean of the vein"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d278f4c9-d20b-4a76-8c5c-4d3e985948b9.jpg?1775937489"
    }
}
