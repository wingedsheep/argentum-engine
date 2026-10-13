package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Evolved Sleeper
 * {B}
 * Creature — Human
 * 1/1
 *
 * {B}: This creature becomes a Human Cleric with base power and toughness 2/2.
 * {1}{B}: If this creature is a Cleric, put a deathtouch counter on it and it becomes a Phyrexian
 * Human Cleric with base power and toughness 3/3.
 * {1}{B}{B}: If this creature is a Phyrexian, put a +1/+1 counter on it, then you draw a card and
 * you lose 1 life.
 *
 * The same self-upgrade chain as Kellan, Planar Trailblazer / Tenth District Hero:
 * - Nothing has a duration (first ruling), so every characteristic change is `Duration.Permanent`.
 * - "becomes a Human Cleric" *overwrites* creature types (second ruling), so it's
 *   `Effects.SetCreatureSubtypes`, not an additive type grant; re-activating the first ability on
 *   the 3/3 Phyrexian Human Cleric makes it a 2/2 Human Cleric again by timestamp.
 * - The "If this creature is a …" checks happen on resolution (third ruling) against projected
 *   state via `Conditions.SourceHasSubtype`, so any route to Cleric / Phyrexian counts (fourth ruling).
 * - Base P/T is Layer 7b set (`Effects.SetBasePowerAndToughness`); counters stay on top (fifth ruling).
 */
val EvolvedSleeper = card("Evolved Sleeper") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human"
    power = 1
    toughness = 1
    oracleText = "{B}: This creature becomes a Human Cleric with base power and toughness 2/2.\n" +
        "{1}{B}: If this creature is a Cleric, put a deathtouch counter on it and it becomes a " +
        "Phyrexian Human Cleric with base power and toughness 3/3.\n" +
        "{1}{B}{B}: If this creature is a Phyrexian, put a +1/+1 counter on it, then you draw a " +
        "card and you lose 1 life."

    // {B}: This creature becomes a Human Cleric with base power and toughness 2/2.
    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.SetCreatureSubtypes(
            subtypes = setOf(Subtype.HUMAN.value, Subtype.CLERIC.value),
            target = EffectTarget.Self,
            duration = Duration.Permanent
        ) then
            Effects.SetBasePowerAndToughness(
                power = 2,
                toughness = 2,
                target = EffectTarget.Self,
                duration = Duration.Permanent
            )
        description = "This creature becomes a Human Cleric with base power and toughness 2/2."
    }

    // {1}{B}: If this creature is a Cleric, put a deathtouch counter on it and it becomes a
    // Phyrexian Human Cleric with base power and toughness 3/3.
    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype.CLERIC),
            then = Effects.AddCounters(CounterType.DEATHTOUCH, 1, EffectTarget.Self) then
                Effects.SetCreatureSubtypes(
                    subtypes = setOf(Subtype.PHYREXIAN.value, Subtype.HUMAN.value, Subtype.CLERIC.value),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                ) then
                Effects.SetBasePowerAndToughness(
                    power = 3,
                    toughness = 3,
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                )
        )
        description = "If this creature is a Cleric, put a deathtouch counter on it and it becomes " +
            "a Phyrexian Human Cleric with base power and toughness 3/3."
    }

    // {1}{B}{B}: If this creature is a Phyrexian, put a +1/+1 counter on it, then you draw a card
    // and you lose 1 life.
    activatedAbility {
        cost = Costs.Mana("{1}{B}{B}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype.PHYREXIAN),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
                Effects.DrawCards(1) then
                Effects.LoseLife(1, EffectTarget.Controller)
        )
        description = "If this creature is a Phyrexian, put a +1/+1 counter on it, then you draw " +
            "a card and you lose 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebeafbc2-0399-4335-8f63-76a3e085e8c6.jpg?1783921332"
        ruling(
            "2022-09-09",
            "None of these abilities has a duration. If one of them resolves, it will remain in " +
                "effect until the game ends, Evolved Sleeper leaves the battlefield, or some " +
                "subsequent effect changes its characteristics, whichever comes first.",
        )
        ruling(
            "2022-09-09",
            "Evolved Sleeper's first two abilities overwrite its power, toughness, and creature " +
                "types. Typically, those abilities are activated in the order they appear on the " +
                "card. However, if Evolved Sleeper is a 3/3 Phyrexian Human Cleric and you activate " +
                "its first ability again, it will become a 2/2 Human Cleric as that ability resolves.",
        )
        ruling(
            "2022-09-09",
            "You can activate Evolved Sleeper's second and third abilities regardless of what " +
                "creature types it is. Each of those abilities checks Evolved Sleeper's creature " +
                "types when that ability resolves. If Evolved Sleeper isn't the appropriate creature " +
                "type at that time, the ability does nothing.",
        )
        ruling(
            "2022-09-09",
            "Evolved Sleeper's second ability checks whether it's a Cleric, and its third ability " +
                "checks whether it's a Phyrexian. It doesn't matter how it became the appropriate " +
                "creature type.",
        )
        ruling(
            "2022-09-09",
            "The effects from the first and second abilities overwrite other effects that set power " +
                "and/or toughness if and only if those effects existed before the ability resolved. " +
                "It will not overwrite effects that modify power or toughness without setting it " +
                "(whether from a static ability, counters, or a resolved spell or ability), nor will " +
                "it overwrite effects that set power and toughness which come into existence after " +
                "the ability resolves. Effects that switch the creature's power and toughness are " +
                "always applied after any other power or toughness changing effects, including this " +
                "one, regardless of the order in which they are created.",
        )
    }
}
