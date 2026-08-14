package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Miles Morales // Ultimate Spider-Man — Marvel's Spider-Man #108 (mythic)
 *
 * Front — Miles Morales · {1}{G} · Legendary Creature — Human Citizen Hero · 1/2
 *   When Miles Morales enters, put a +1/+1 counter on each of up to two target creatures.
 *   {3}{R}{G}{W}: Transform Miles Morales. Activate only as a sorcery.
 *
 * Back — Ultimate Spider-Man · Legendary Creature — Spider Human Hero · 4/3
 *   First strike, haste
 *   Camouflage — {2}: Put a +1/+1 counter on Ultimate Spider-Man. He gains hexproof and becomes
 *   colorless until end of turn.
 *   Whenever you attack, double the number of each kind of counter on each Spider and legendary
 *   creature you control.
 *
 * Modeled as a transforming double-faced creature (two [card] faces joined by
 * [CardDefinition.doubleFacedCreature]); the front owns the sorcery-speed [TransformEffect] flip.
 * The back is a transformed face with no mana cost, so its colors come from a color indicator
 * (CR 204) — `colorIndicator = "GRW"` to match Scryfall's G/R/W back-face colors.
 *
 *  - ETB (front): up-to-two [Targets.UpToCreatures] fanned out with [ForEachTargetEffect] so each
 *    chosen creature receives exactly one +1/+1 counter.
 *  - Camouflage (back): [Effects.Composite] of a permanent +1/+1 counter, [Effects.GrantHexproof]
 *    (until end of turn) and [Effects.ChangeColor] with an empty color set = colorless (until end
 *    of turn). Only the hexproof and colorless clauses are end-of-turn; the counter is permanent.
 *  - Attack trigger (back): [Triggers.YouAttack] + [Effects.ForEachInGroup] applying
 *    [Effects.DoubleAllCounters] (counterType null → each kind of counter) to every Spider or
 *    legendary creature you control.
 */

private val MilesMoralesFront = card("Miles Morales") {
    manaCost = "{1}{G}"
    colorIdentity = "GRW"
    typeLine = "Legendary Creature — Human Citizen Hero"
    power = 1
    toughness = 2
    oracleText = "When Miles Morales enters, put a +1/+1 counter on each of up to two target " +
        "creatures.\n" +
        "{3}{R}{G}{W}: Transform Miles Morales. Activate only as a sorcery."

    // When Miles Morales enters, put a +1/+1 counter on each of up to two target creatures.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = Targets.UpToCreatures(2)
        effect = ForEachTargetEffect(
            listOf(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)))
        )
        description = "When Miles Morales enters, put a +1/+1 counter on each of up to two target creatures."
    }

    // {3}{R}{G}{W}: Transform Miles Morales. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{3}{R}{G}{W}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Miles Morales. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "108"
        artist = "L.A. Draws"
        flavorText = "\"You can't call yourself Spider-Man without taking a leap of faith. It's a lot to live up to. But I'm creating an all-new legacy.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f8b4d9b-208a-4673-a617-5e3edd069c33.jpg?1783905331"
    }
}

private val UltimateSpiderMan = card("Ultimate Spider-Man") {
    manaCost = ""
    colorIdentity = "GRW"
    colorIndicator = "GRW" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 4
    toughness = 3
    oracleText = "First strike, haste\n" +
        "Camouflage — {2}: Put a +1/+1 counter on Ultimate Spider-Man. He gains hexproof and " +
        "becomes colorless until end of turn.\n" +
        "Whenever you attack, double the number of each kind of counter on each Spider and " +
        "legendary creature you control."

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)

    // Camouflage — {2}: Put a +1/+1 counter on Ultimate Spider-Man. He gains hexproof and becomes
    // colorless until end of turn.
    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.GrantHexproof(EffectTarget.Self, Duration.EndOfTurn),
            Effects.ChangeColor(EffectTarget.Self, emptySet(), Duration.EndOfTurn)
        )
        description = "Camouflage — Put a +1/+1 counter on Ultimate Spider-Man. He gains hexproof and becomes colorless until end of turn."
    }

    // Whenever you attack, double the number of each kind of counter on each Spider and legendary
    // creature you control.
    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Creature.youControl().withSubtype("Spider") or
                    GameObjectFilter.Creature.youControl().legendary()
            ),
            Effects.DoubleAllCounters(EffectTarget.Self)
        )
        description = "Whenever you attack, double the number of each kind of counter on each Spider and legendary creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "108"
        artist = "L.A. Draws"
        imageUri = "https://cards.scryfall.io/normal/back/9/f/9f8b4d9b-208a-4673-a617-5e3edd069c33.jpg?1783905331"
    }
}

val MilesMorales: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = MilesMoralesFront,
    backFace = UltimateSpiderMan,
)
