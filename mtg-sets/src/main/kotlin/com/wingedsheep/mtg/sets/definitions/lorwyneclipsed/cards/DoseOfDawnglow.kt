package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Dose of Dawnglow
 * {4}{B}
 * Instant
 *
 * Return target creature card from your graveyard to the battlefield.
 * Then if it isn't your main phase, blight 2.
 * (Put two -1/-1 counters on a creature you control.)
 */
val DoseOfDawnglow = card("Dose of Dawnglow") {
    manaCost = "{4}{B}"
    typeLine = "Instant"
    oracleText = "Return target creature card from your graveyard to the battlefield. " +
        "Then if it isn't your main phase, blight 2. (Put two -1/-1 counters on a creature you control.)"

    spell {
        val creature = target("target creature card from your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = CompositeEffect(
            listOf(
                Effects.PutOntoBattlefield(creature),
                ConditionalEffect(
                    condition = Conditions.Not(Conditions.IsYourMainPhase),
                    effect = EffectPatterns.blight(2)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Quintin Gleim"
        flavorText = "A lifetime of mischief gave way to a moment of tenderness."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47414323-ca30-45b7-a0b2-6668312bee04.jpg?1765883451"
    }
}
