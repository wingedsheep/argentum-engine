package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Square Up — Strixhaven: School of Mages #238 (canonical printing)
 * {1}{G/U} · Instant
 *
 * Target creature has base power and toughness 4/4 until end of turn.
 *
 * A layer 7b set-values effect ([Effects.SetBasePowerAndToughness]); counters and pumps still
 * apply on top of it, per the rulings.
 */
val SquareUp = card("Square Up") {
    manaCost = "{1}{G/U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Target creature has base power and toughness 4/4 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.SetBasePowerAndToughness(4, 4, creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "238"
        artist = "Manuel Castañón"
        flavorText = "Sometimes the most radical thing you can do is think inside the box."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d548d5a-1dd7-448a-8483-249572e08c47.jpg?1783927290"
        ruling("2021-04-16", "Square Up overwrites all previous effects that set the target creature's power and toughness to specific values. Other effects that set its power or toughness to specific values that start to apply after Square Up resolves will overwrite this effect.")
        ruling("2021-04-16", "Any effects that modify a creature's power and/or toughness without setting them to a specific value (i.e. ones that don't affect base power and/or toughness) will apply after its base power and toughness are set, regardless of the order those effects were created. The same is true for counters that modify its power and toughness.")
    }
}
