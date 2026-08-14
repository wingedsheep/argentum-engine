package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Abundant Maw
 * {8}
 * Creature — Eldrazi Leech
 * 6/4
 *
 * Emerge {6}{B}
 * When you cast this spell, target opponent loses 3 life and you gain 3 life.
 *
 * Implementation notes:
 * - Emerge is the engine keyword (CR 702.119) via the `emerge(cost)` helper.
 * - The drain is a *cast* trigger — it resolves before the creature and still resolves if the
 *   spell is countered.
 * - Two independent fixed amounts, not a linked drain: the opponent loses 3 (going below 0 if they
 *   were at less) and you gain 3 regardless. `DrainLife` would wrongly cap the gain at the life
 *   actually lost, which is Exsanguinate's wording, not this one's.
 */
val AbundantMaw = card("Abundant Maw") {
    manaCost = "{8}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Leech"
    power = 6
    toughness = 4
    oracleText = "Emerge {6}{B} (You may cast this spell by sacrificing a creature and paying the " +
        "emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, target opponent loses 3 life and you gain 3 life."

    emerge("{6}{B}")

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        val victim = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            Effects.LoseLife(3, target = victim),
            Effects.GainLife(3),
        )
        description = "When you cast this spell, target opponent loses 3 life and you gain 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a4e7ef7-7958-4d7c-b319-4d3db7955002.jpg?1783937529"
    }
}
