package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani Unyielding — Aether Revolt #127
 * {4}{G}{W} · Legendary Planeswalker — Ajani · Loyalty 4
 *
 * +2: Reveal the top three cards of your library. Put all nonland permanent cards revealed this
 * way into your hand and the rest on the bottom of your library in any order.
 * −2: Exile target creature. Its controller gains life equal to its power.
 * −9: Put five +1/+1 counters on each creature you control and five loyalty counters on each
 * other planeswalker you control.
 *
 * The −2 is Swords to Plowshares' shape: the life gain is sequenced before the exile so it reads
 * the creature's projected power and controller while it is still on the battlefield — the
 * last-known value the ruling asks for (see SwordsToPlowshares for the reasoning).
 *
 * The −9 is Ajani, the Greathearted's fan-out with five counters: a planeswalker that is also a
 * creature gets both kinds, and Ajani himself is excluded from the loyalty half.
 */
val AjaniUnyielding = card("Ajani Unyielding") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 4
    oracleText = "+2: Reveal the top three cards of your library. Put all nonland permanent cards revealed " +
        "this way into your hand and the rest on the bottom of your library in any order.\n" +
        "−2: Exile target creature. Its controller gains life equal to its power.\n" +
        "−9: Put five +1/+1 counters on each creature you control and five loyalty counters on each " +
        "other planeswalker you control."

    loyaltyAbility(+2) {
        effect = Patterns.Library.revealTopPutAllMatchingToHand(
            count = 3,
            filter = GameObjectFilter.NonlandPermanent,
            restOrder = CardOrder.ControllerChooses
        )
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainLife(DynamicAmounts.powerOf(creature), EffectTarget.TargetController) then
            Effects.Exile(creature)
    }

    loyaltyAbility(-9) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 5, EffectTarget.IterationEntity),
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Planeswalker.youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.LOYALTY, 5, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "127"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23b82ee9-2ac6-4b81-8c64-dd4f47c2d8cb.jpg?1783936737"
        ruling(
            "2017-02-09",
            "The amount of life gained from Ajani's second ability is equal to power of the creature as it " +
                "last existed on the battlefield."
        )
    }
}
