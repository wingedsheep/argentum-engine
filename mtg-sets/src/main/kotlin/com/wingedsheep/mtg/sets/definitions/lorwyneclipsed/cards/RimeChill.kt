package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Rime Chill
 * {6}{U}
 * Instant
 *
 * Vivid — This spell costs {1} less to cast for each color among permanents you control.
 * Tap up to two target creatures. Put a stun counter on each of them.
 * Draw a card.
 */
val RimeChill = card("Rime Chill") {
    manaCost = "{6}{U}"
    typeLine = "Instant"
    oracleText = "Vivid — This spell costs {1} less to cast for each color among permanents you control.\n" +
            "Tap up to two target creatures. Put a stun counter on each of them. (If a permanent with a stun counter " +
            "would become untapped, remove one from it instead.)\n" +
            "Draw a card."

    vividCostReduction()

    spell {
        target("up to two target creatures", TargetCreature(count = 2, optional = true))
        effect = ForEachTargetEffect(
            listOf(
                Effects.Tap(EffectTarget.ContextTarget(0)),
                Effects.AddCounters("STUN", 1, EffectTarget.ContextTarget(0))
            )
        ).then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9a425f4-2103-4f96-88a0-91fe554037d7.jpg?1767957062"

        ruling(
            "2025-11-17",
            "If all of the target creatures are illegal targets as Rime Chill tries to resolve, it won't " +
                    "resolve and none of its effects will happen. You won't draw a card."
        )
    }
}
