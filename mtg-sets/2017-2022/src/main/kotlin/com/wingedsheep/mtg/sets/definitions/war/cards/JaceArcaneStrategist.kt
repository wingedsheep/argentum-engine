package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jace, Arcane Strategist
 * {4}{U}{U}
 * Legendary Planeswalker — Jace
 * Starting Loyalty: 4
 *
 * Whenever you draw your second card each turn, put a +1/+1 counter on target creature you control.
 * +1: Draw a card.
 * −7: Creatures you control can't be blocked this turn.
 *
 * The static trigger is Faerie Vandal's `drawsNth(2)` with Mantle of Tides' targeted rider. The −7
 * visits every creature you control at resolution and grants each the `CANT_BE_BLOCKED` flag until
 * end of turn — a floating effect per creature, so it outlives Jace leaving the battlefield (the
 * ruling below).
 */
val JaceArcaneStrategist = card("Jace, Arcane Strategist") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 4
    oracleText = "Whenever you draw your second card each turn, put a +1/+1 counter on target creature you control.\n" +
        "+1: Draw a card.\n" +
        "−7: Creatures you control can't be blocked this turn."

    // Whenever you draw your second card each turn, put a +1/+1 counter on target creature you control.
    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    // +1: Draw a card.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1)
    }

    // −7: Creatures you control can't be blocked this turn.
    loyaltyAbility(-7) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "270"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6a857fb-159f-40c4-8988-da91d8521a60.jpg?1783933359"

        ruling("2019-05-03", "Jace's first ability can trigger only once each turn. It doesn't matter whether Jace was on the battlefield when the first card was drawn.")
        ruling("2019-05-03", "If an effect instructs you to draw multiple cards, Jace's first ability triggers after you draw whichever is the second one for the turn (if any). You choose a target for the ability after you've drawn all of the cards.")
        ruling("2019-05-03", "If a spell or ability causes you to put cards into your hand without specifically using the word \"draw,\" it's not a card drawn.")
        ruling("2019-05-03", "Once Jace's last ability has resolved, its effect applies even if Jace has left the battlefield.")
    }
}
