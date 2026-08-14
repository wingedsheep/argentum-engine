package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Captivating Vampire
 * {1}{B}{B}
 * Creature — Vampire
 * 2/2
 *
 * Other Vampire creatures you control get +1/+1.
 * Tap five untapped Vampires you control: Gain control of target creature. It becomes a Vampire in
 * addition to its other types.
 *
 * The activation cost has no {T} symbol, so Captivating Vampire itself counts as one of the five
 * (`excludeSelf` stays false) and none of the five need to have been under your control since your
 * most recent turn began. Both halves of the effect are permanent — the M11 ruling is explicit that
 * neither the control change nor the added Vampire type has a duration, and both survive Captivating
 * Vampire leaving the battlefield, so `Duration.Permanent` (the default on both facades) is correct.
 */
val CaptivatingVampire = card("Captivating Vampire") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 2
    oracleText = "Other Vampire creatures you control get +1/+1.\n" +
        "Tap five untapped Vampires you control: Gain control of target creature. It becomes a " +
        "Vampire in addition to its other types."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.TapPermanents(
            count = 5,
            filter = GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl()
        )
        target = Targets.Creature
        effect = Effects.GainControl(EffectTarget.ContextTarget(0))
            .then(Effects.AddCreatureType("Vampire", EffectTarget.ContextTarget(0)))
        description = "Gain control of target creature. It becomes a Vampire in addition to its other types."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f99b0735-cc3c-48dc-a7ad-b7d9eea45e54.jpg?1783941818"

        ruling(
            "2010-08-15",
            "Since Captivating Vampire's activated ability doesn't have a tap symbol in its cost, " +
                "you can tap a Vampire (including Captivating Vampire itself) that hasn't been under " +
                "your control since your most recent turn began to pay the cost."
        )
        ruling(
            "2010-08-15",
            "The effect of Captivating Vampire's activated ability has no duration. You retain " +
                "control of the affected creature until the game ends, the creature leaves the " +
                "battlefield, or a later effect causes someone else to gain control of it. It doesn't " +
                "matter whether Captivating Vampire remains on the battlefield. Similarly, the " +
                "affected creature remains a Vampire in addition to its other types until the game " +
                "ends, the creature leaves the battlefield, or a later effect changes its types or subtypes."
        )
        ruling(
            "2010-08-15",
            "Gaining control of a creature doesn't cause you gain control of any Auras or Equipment " +
                "attached to it."
        )
    }
}
