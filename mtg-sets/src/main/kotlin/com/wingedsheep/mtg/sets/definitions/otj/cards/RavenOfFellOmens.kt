package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raven of Fell Omens
 * {1}{B}
 * Creature — Bird
 * 1/2
 *
 * Flying
 * Whenever you commit a crime, each opponent loses 1 life and you gain 1 life.
 * This ability triggers only once each turn.
 */
val RavenOfFellOmens = card("Raven of Fell Omens") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever you commit a crime, each opponent loses 1 life and you gain 1 life. " +
        "This ability triggers only once each turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = Effects.Composite(
            listOf(
                LoseLifeEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                GainLifeEffect(1, EffectTarget.Controller)
            )
        )
        description = "Whenever you commit a crime, each opponent loses 1 life and you gain 1 life. " +
            "This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Justin Cornell"
        flavorText = "A raven sighted at midnight often means there's unkindness on the way."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2df3cb4-1658-450a-912a-df336706acdc.jpg?1712355645"
    }
}
