package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Warden of the First Tree
 * {G}
 * Creature — Human
 * 1/1
 *
 * {1}{W/B}: This creature becomes a Human Warrior with base power and toughness 3/3.
 * {2}{W/B}{W/B}: If this creature is a Warrior, it becomes a Human Spirit Warrior with trample
 * and lifelink.
 * {3}{W/B}{W/B}{W/B}: If this creature is a Spirit, put five +1/+1 counters on it.
 *
 * The same upgrade chain as Ascendant Spirit:
 * - Neither "becomes" ability has a duration (first ruling), so every change is `Duration.Permanent`.
 * - "becomes a Human Warrior" overwrites creature types (second ruling) — `SetCreatureSubtypes` in
 *   layer 4; base P/T is a layer-7b set, so counters and pumps still apply on top (fourth ruling).
 * - The Warrior / Spirit checks happen on resolution against projected subtypes.
 */
val WardenOfTheFirstTree = card("Warden of the First Tree") {
    manaCost = "{G}"
    colorIdentity = "BGW"
    typeLine = "Creature — Human"
    power = 1
    toughness = 1
    oracleText = "{1}{W/B}: This creature becomes a Human Warrior with base power and toughness 3/3.\n" +
        "{2}{W/B}{W/B}: If this creature is a Warrior, it becomes a Human Spirit Warrior with trample " +
        "and lifelink.\n" +
        "{3}{W/B}{W/B}{W/B}: If this creature is a Spirit, put five +1/+1 counters on it."

    // {1}{W/B}: This creature becomes a Human Warrior with base power and toughness 3/3.
    activatedAbility {
        cost = Costs.Mana("{1}{W/B}")
        effect = Effects.SetCreatureSubtypes(
            subtypes = setOf(Subtype.HUMAN.value, Subtype.WARRIOR.value),
            target = EffectTarget.Self,
            duration = Duration.Permanent
        ) then
            Effects.SetBasePowerAndToughness(
                power = 3,
                toughness = 3,
                target = EffectTarget.Self,
                duration = Duration.Permanent
            )
        description = "This creature becomes a Human Warrior with base power and toughness 3/3."
    }

    // {2}{W/B}{W/B}: If this creature is a Warrior, it becomes a Human Spirit Warrior with trample
    // and lifelink.
    activatedAbility {
        cost = Costs.Mana("{2}{W/B}{W/B}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype.WARRIOR),
            then = Effects.SetCreatureSubtypes(
                subtypes = setOf(Subtype.HUMAN.value, Subtype.SPIRIT.value, Subtype.WARRIOR.value),
                target = EffectTarget.Self,
                duration = Duration.Permanent
            ) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self, Duration.Permanent) then
                Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self, Duration.Permanent)
        )
        description = "If this creature is a Warrior, it becomes a Human Spirit Warrior with " +
            "trample and lifelink."
    }

    // {3}{W/B}{W/B}{W/B}: If this creature is a Spirit, put five +1/+1 counters on it.
    activatedAbility {
        cost = Costs.Mana("{3}{W/B}{W/B}{W/B}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype.SPIRIT),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 5, EffectTarget.Self)
        )
        description = "If this creature is a Spirit, put five +1/+1 counters on it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "143"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e0c6628-04f1-4800-baa8-bcaefe64f59f.jpg?1783938676"
        ruling(
            "2014-11-24",
            "Neither the first nor the second ability has a duration. If one of them resolves, it " +
                "will remain in effect until Warden of the First Tree leaves the battlefield or some " +
                "subsequent effect changes its characteristics.",
        )
        ruling(
            "2014-11-24",
            "The first and second activated abilities cause Warden of the First Tree to lose any " +
                "other creature types it has. It retains any card types or supertypes it may have.",
        )
        ruling(
            "2014-11-24",
            "The first ability overwrites any previous effects that set the creature's base power " +
                "and toughness to specific values. Any power- or toughness-setting effects that start " +
                "to apply after the first ability resolves will overwrite this effect.",
        )
        ruling(
            "2014-11-24",
            "Effects that modify Warden of the First Tree's power or toughness, such as the one " +
                "created by Titanic Growth, will apply to Warden of the First Tree no matter when they " +
                "started to take effect. The same is true for any +1/+1 counters on Warden of the " +
                "First Tree and effects that switch its power and toughness.",
        )
    }
}
