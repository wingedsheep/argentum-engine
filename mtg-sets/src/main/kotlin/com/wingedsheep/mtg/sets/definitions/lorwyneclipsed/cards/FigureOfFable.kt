package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Figure of Fable
 * {G/W}
 * Creature — Kithkin
 * 1/1
 *
 * {G/W}: This creature becomes a Kithkin Scout with base power and toughness 2/3.
 * {1}{G/W}{G/W}: If this creature is a Scout, it becomes a Kithkin Soldier with base power and toughness 4/5.
 * {3}{G/W}{G/W}{G/W}: If this creature is a Soldier, it becomes a Kithkin Avatar with base power and toughness 7/8 and protection from each of your opponents.
 *
 * Per Scryfall ruling (2025-11-17): each ability checks Figure of Fable's creature
 * types when it resolves, so the conditional gate lives inside the effect rather
 * than as an activation restriction. The ability is always legal to activate; it
 * does nothing on resolution if the source isn't currently the prerequisite form.
 */
val FigureOfFable = card("Figure of Fable") {
    manaCost = "{G/W}"
    typeLine = "Creature — Kithkin"
    power = 1
    toughness = 1
    oracleText = "{G/W}: This creature becomes a Kithkin Scout with base power and toughness 2/3.\n" +
        "{1}{G/W}{G/W}: If this creature is a Scout, it becomes a Kithkin Soldier with base power and toughness 4/5.\n" +
        "{3}{G/W}{G/W}{G/W}: If this creature is a Soldier, it becomes a Kithkin Avatar with base power and toughness 7/8 and protection from each of your opponents."

    activatedAbility {
        cost = Costs.Mana("{G/W}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 3,
            creatureTypes = setOf("Kithkin", "Scout"),
            duration = Duration.Permanent
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G/W}{G/W}")
        effect = ConditionalEffect(
            condition = Conditions.SourceHasSubtype(Subtype.SCOUT),
            effect = Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 4,
                toughness = 5,
                creatureTypes = setOf("Kithkin", "Soldier"),
                duration = Duration.Permanent
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G/W}{G/W}{G/W}")
        effect = ConditionalEffect(
            condition = Conditions.SourceHasSubtype(Subtype.SOLDIER),
            effect = Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 7,
                toughness = 8,
                keywords = setOf(Keyword.PROTECTION_FROM_EACH_OPPONENT),
                creatureTypes = setOf("Kithkin", "Avatar"),
                duration = Duration.Permanent
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Omar Rayyan"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0ef33dd-5f6d-48fa-8ef6-a8092868d50f.jpg?1759144844"
        ruling("2025-11-17", "None of these abilities have durations. If one of them resolves, it will remain in effect until the game ends, Figure of Fable leaves the battlefield, or some subsequent effect changes its characteristics, whichever comes first.")
        ruling("2025-11-17", "You can activate Figure of Fable's abilities regardless of what creature types it currently has. Each ability checks Figure of Fable's creature types when it resolves. If Figure of Fable doesn't have the appropriate creature type at that time, the ability will do nothing.")
        ruling("2025-11-17", "Figure of Fable's abilities overwrite its existing creature types. For example, once Figure of Fable's second ability resolves, if it was a Scout when the ability resolves, Figure of Fable will be a Kithkin Soldier. It won't have the Scout creature type.")
    }
}
