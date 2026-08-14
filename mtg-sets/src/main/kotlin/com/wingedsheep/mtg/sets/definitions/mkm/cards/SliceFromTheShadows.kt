package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Slice from the Shadows — Murders at Karlov Manor #103
 * {X}{B} · Instant
 *
 * This spell can't be countered.
 * Target creature gets -X/-X until end of turn.
 *
 * A Death Wind that dodges counterspells. The reminder text calls out the interesting case: ward is a
 * *counter* the spell, so the ward trigger still goes on the stack and still asks you to pay, but if
 * you decline, the counter attempt simply does nothing and Slice resolves anyway (CR 702.21b — the
 * ward ability counters the spell, and `cantBeCountered` beats it).
 *
 * X is locked in on cast, so shrinking the target after announcement doesn't change the -X/-X, and a
 * responding pump spell can still save the creature. -X/-X is a stat modification in Layer 7c, not
 * damage, so it kills through indestructible and doesn't count as damage for lifelink or triggers.
 */
val SliceFromTheShadows = card("Slice from the Shadows") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell can't be countered. (This includes by the ward ability.)\n" +
        "Target creature gets -X/-X until end of turn."
    cantBeCountered = true

    spell {
        val creature = target("creature", Targets.Creature)
        val negX = DynamicAmount.Multiply(DynamicAmount.XValue, -1)
        effect = Effects.ModifyStats(negX, negX, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Lie Setiawan"
        flavorText = "\"Proft has been poking his nose where it doesn't belong. " +
            "I want him out of the way.\"\n—Judith, to Massacre Girl"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/317800b3-6b2d-4de6-8e44-7e54dd623055.jpg?1783912894"

        ruling(
            "2024-02-02",
            "If you target a creature with ward, you may still pay the ward cost, but Slice from " +
                "the Shadows won't be countered even if you don't."
        )
    }
}
