package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Katsumasa, the Animator — Neon Dynasty Commander #15 (canonical printing)
 * {2}{U}{U} · Legendary Creature — Moonfolk Artificer · 3/3
 *
 * Flying
 * {2}{U}: Until end of turn, target noncreature artifact you control becomes an artifact creature
 * and gains flying. If it's not a Vehicle, it has base power and toughness 1/1 until end of turn.
 * At the beginning of your upkeep, put a +1/+1 counter on each of up to three target noncreature
 * artifacts.
 *
 * The animate is the Tezzeret, Betrayer of Flesh shape with an end-of-turn duration: whether the
 * target is a Vehicle is checked as the ability resolves — a Vehicle only gains the creature type
 * and keeps its printed power and toughness, anything else also gets base 1/1. Becoming a creature
 * this way isn't crewing.
 */
val KatsumasaTheAnimator = card("Katsumasa, the Animator") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Moonfolk Artificer"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "{2}{U}: Until end of turn, target noncreature artifact you control becomes an artifact " +
        "creature and gains flying. If it's not a Vehicle, it has base power and toughness 1/1 until " +
        "end of turn.\n" +
        "At the beginning of your upkeep, put a +1/+1 counter on each of up to three target " +
        "noncreature artifacts."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.notCreature().youControl()))
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE),
                artifact
            ),
            then = Effects.AddCardType("CREATURE", artifact, Duration.EndOfTurn),
            otherwise = Effects.BecomeCreature(
                target = artifact,
                power = 1,
                toughness = 1,
                duration = Duration.EndOfTurn
            )
        ) then Effects.GrantKeyword(Keyword.FLYING, artifact)
        description = "{2}{U}: Until end of turn, target noncreature artifact you control becomes an " +
            "artifact creature and gains flying. If it's not a Vehicle, it has base power and toughness " +
            "1/1 until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        targets(TargetFilter(GameObjectFilter.Artifact.notCreature()), count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab4b5ff5-fd6a-4948-9c54-440bf6324bd1.jpg?1783923994"
    }
}
