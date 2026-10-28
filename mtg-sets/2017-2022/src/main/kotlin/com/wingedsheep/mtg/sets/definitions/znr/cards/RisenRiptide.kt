package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Risen Riptide — Zendikar Rising #73
 * {2}{U} · Creature — Elemental · 0/5
 *
 * Whenever you cast a kicked spell, this creature has base power and toughness 5/5 until end of turn.
 *
 * The 5/5 is a layer 7b set-values effect, so counters and pump effects still apply on top of it.
 */
val RisenRiptide = card("Risen Riptide") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 5
    oracleText = "Whenever you cast a kicked spell, this creature has base power and toughness 5/5 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
        effect = Effects.SetBasePowerAndToughness(5, 5, EffectTarget.Self, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Zezhou Chen"
        flavorText = "\"Most days the sea is my friend. But only most days.\"\n—Jori En, expedition leader"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6455009-030c-4a33-a60b-80863d8f8aaf.jpg?1783929391"
        ruling(
            "2020-09-25",
            "Risen Riptide's ability overwrites any previous effects that set its power and/or toughness to " +
                "specific values. Other effects that set these characteristics to specific values that start to " +
                "apply after the ability resolves will overwrite that part of the effect."
        )
        ruling(
            "2020-09-25",
            "Effects that modify Risen Riptide's power or toughness without setting it will apply to its new " +
                "base power and toughness no matter when they started to take effect. The same is true for " +
                "counters that change the creature's power or toughness."
        )
    }
}
