package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ms. Bumbleflower — Bloomburrow Commander #3
 * {1}{G}{W}{U} · Legendary Creature — Rabbit Citizen · 1/5
 *
 * Vigilance
 * Whenever you cast a spell, target opponent draws a card. Put a +1/+1 counter on target creature.
 * It gains flying until end of turn. If this is the second time this ability has resolved this
 * turn, you draw two cards.
 *
 * The Harvestrite Host / Sephiroth shape: [IncrementAbilityResolutionCountEffect] bumps the
 * source's per-turn resolution tally after the main clauses, then
 * [Conditions.SourceAbilityResolvedNTimes] (an equality) pays out only on the second resolution.
 * A trigger that fizzles (all targets illegal) never resolves, so it never reaches the increment.
 */
val MsBumbleflower = card("Ms. Bumbleflower") {
    manaCost = "{1}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Rabbit Citizen"
    power = 1
    toughness = 5
    oracleText = "Vigilance\n" +
        "Whenever you cast a spell, target opponent draws a card. Put a +1/+1 counter on target " +
        "creature. It gains flying until end of turn. If this is the second time this ability has " +
        "resolved this turn, you draw two cards."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.casts()
        val opponent = target(Targets.Opponent)
        val creature = target(TargetFilter.Creature)
        effect = Effects.DrawCards(1, opponent) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.FLYING, creature) then
            IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.DrawCards(2),
            )
        description = "Whenever you cast a spell, target opponent draws a card. Put a +1/+1 counter " +
            "on target creature. It gains flying until end of turn. If this is the second time this " +
            "ability has resolved this turn, you draw two cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42bbedc1-6b83-46b4-8b3b-a4e05ce77d87.jpg?1783910739"
        ruling(
            "2024-07-26",
            "Ms. Bumbleflower's last ability resolves before the spell that caused it to trigger. " +
                "It resolves even if that spell is countered.",
        )
    }
}
