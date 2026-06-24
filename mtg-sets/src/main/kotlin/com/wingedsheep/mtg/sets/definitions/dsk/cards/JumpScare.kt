package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jump Scare
 * {W}
 * Instant
 * Until end of turn, target creature gets +2/+2, gains flying, and becomes a Horror
 * enchantment creature in addition to its other types.
 *
 * A single until-end-of-turn buff on one target creature, composed from atomic effects:
 *   +2/+2 (ModifyStats, EndOfTurn) + grant flying (GrantKeyword) + add the "Horror" creature
 *   subtype + add the "enchantment" card type. The creature already has the creature type, so
 *   the type changes ("becomes a Horror enchantment creature in addition to its other types")
 *   reduce to adding the Horror subtype and the enchantment card type for the turn. All four
 *   pieces use [Duration.EndOfTurn] so they expire together in the cleanup step.
 */
val JumpScare = card("Jump Scare") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +2/+2, gains flying, and becomes a " +
        "Horror enchantment creature in addition to its other types."

    spell {
        target = Targets.Creature
        effect = Effects.Composite(
            listOf(
                Effects.ModifyStats(2, 2, target = EffectTarget.ContextTarget(0), duration = Duration.EndOfTurn),
                Effects.GrantKeyword(Keyword.FLYING, target = EffectTarget.ContextTarget(0), duration = Duration.EndOfTurn),
                Effects.AddCreatureType("Horror", target = EffectTarget.ContextTarget(0), duration = Duration.EndOfTurn),
                Effects.AddCardType("ENCHANTMENT", target = EffectTarget.ContextTarget(0), duration = Duration.EndOfTurn)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "John Tedrick"
        flavorText = "\"Boo!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2dd8903-31ca-470d-b2ff-280f6d40c794.jpg?1726285919"
    }
}
