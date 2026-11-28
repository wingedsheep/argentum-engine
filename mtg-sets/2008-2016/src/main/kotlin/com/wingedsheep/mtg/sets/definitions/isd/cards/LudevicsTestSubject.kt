package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ludevic's Test Subject // Ludevic's Abomination (Innistrad #64)
 * {1}{U}
 * Creature — Lizard Egg 0/3 // Creature — Lizard Horror 13/13
 *
 * Front — "Defender
 *          {1}{U}: Put a hatchling counter on this creature. Then if there are five or more hatchling
 *          counters on it, remove all of them and transform it."
 * Back  — "Trample"
 *
 * The threshold is checked on resolution, after the counter goes on, so the fifth activation to
 * resolve is the one that transforms. Only hatchling counters are removed; any other counters stay
 * on the permanent through the transform (same object).
 *
 * The back face has no mana cost, so its blue comes from a color indicator (CR 204).
 */
private val LudevicsTestSubjectFront = card("Ludevic's Test Subject") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Lizard Egg"
    power = 0
    toughness = 3
    oracleText = "Defender\n{1}{U}: Put a hatchling counter on this creature. Then if there are five or " +
        "more hatchling counters on it, remove all of them and transform it."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.AddCounters(CounterType.HATCHLING, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.HATCHLING, 5),
                then = Effects.RemoveAllCountersOfType(CounterType.HATCHLING, EffectTarget.Self) then
                    Effects.Transform(EffectTarget.Self),
            )
        description = "Put a hatchling counter on this creature. Then if there are five or more hatchling " +
            "counters on it, remove all of them and transform it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebf5e16f-a8bd-419f-b5ca-8c7fce09c4f1.jpg?1783940976"
    }
}

private val LudevicsAbomination = card("Ludevic's Abomination") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Lizard Horror"
    power = 13
    toughness = 13
    oracleText = "Trample"

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Nils Hamm"
        flavorText = "After several frustrating experiments, the visionary Ludevic realized he needed to " +
            "create a monster that fed on torch-wielding mobs."
        imageUri = "https://cards.scryfall.io/normal/back/e/b/ebf5e16f-a8bd-419f-b5ca-8c7fce09c4f1.jpg?1783940976"
    }
}

val LudevicsTestSubject: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = LudevicsTestSubjectFront,
    backFace = LudevicsAbomination,
)
