package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wild Shape
 * {G}
 * Instant
 *
 * Choose one. Until end of turn, target creature you control has that base power and toughness,
 * becomes that creature type, and gains that ability.
 * • 1/3 Turtle with hexproof.
 * • 1/5 Spider with reach.
 * • 3/3 Elephant with trample.
 *
 * A choose-one `modal`; each mode targets a creature you control (mode and target are both chosen
 * on announcement, per ruling) and applies the Mordenkainen's Polymorph stack until end of turn:
 * replace creature subtypes (Layer 4), set base P/T (Layer 7b), grant the keyword (Layer 6).
 */
val WildShape = card("Wild Shape") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one. Until end of turn, target creature you control has that base power and " +
        "toughness, becomes that creature type, and gains that ability.\n" +
        "• 1/3 Turtle with hexproof.\n" +
        "• 1/5 Spider with reach.\n" +
        "• 3/3 Elephant with trample."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature you control becomes a 1/3 Turtle with hexproof until end of turn") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = wildShapeForm(creature, "Turtle", 1, 3, Keyword.HEXPROOF)
            }
            mode("Target creature you control becomes a 1/5 Spider with reach until end of turn") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = wildShapeForm(creature, "Spider", 1, 5, Keyword.REACH)
            }
            mode("Target creature you control becomes a 3/3 Elephant with trample until end of turn") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = wildShapeForm(creature, "Elephant", 3, 3, Keyword.TRAMPLE)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Sam Guay"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec9738f1-b2e7-45a5-b235-290256aabed0.jpg?1783926453"
        ruling(
            "2021-07-23",
            "Both the target creature and the mode are chosen on announcement. You can't choose a different " +
                "form if something changes in response to the spell."
        )
        ruling(
            "2021-07-23",
            "The creature loses all other creature types, but it retains its abilities. Wild Shape will " +
                "overwrite any previous effects that set the creature's power and toughness to specific numbers. " +
                "Effects that otherwise modify the target creature's power and toughness will still apply no " +
                "matter when they took effect. The same is true for +1/+1 counters."
        )
    }
}

private fun wildShapeForm(
    target: EffectTarget,
    subtype: String,
    power: Int,
    toughness: Int,
    keyword: Keyword
): Effect =
    Effects.SetCreatureSubtypes(
        subtypes = setOf(subtype),
        target = target,
        duration = Duration.EndOfTurn
    ) then
        Effects.SetBasePowerAndToughness(
            power = power,
            toughness = toughness,
            target = target,
            duration = Duration.EndOfTurn
        ) then
        Effects.GrantKeyword(
            keyword = keyword,
            target = target,
            duration = Duration.EndOfTurn
        )
