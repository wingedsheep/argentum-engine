package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Communal Brewing {2}{G}
 * Enchantment
 *
 * "Cards drawn this way" is the growth of the opponents' cards-drawn-this-turn tally across the
 * draws (an empty library draws nothing). The cast trigger puts the +1/+1 counters on the creature
 * spell on the stack; they travel with it onto the battlefield (Long List of the Ents precedent).
 * X reads the ingredient counters as the trigger resolves, with LKI if Communal Brewing has left.
 */
val CommunalBrewing = card("Communal Brewing") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, any number of target opponents each draw a card. " +
        "Put an ingredient counter on this enchantment, then put an ingredient counter on it for " +
        "each card drawn this way.\n" +
        "Whenever you cast a creature spell, that creature enters with X additional +1/+1 counters " +
        "on it, where X is the number of ingredient counters on this enchantment."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetOpponent(unlimited = true))
        effect = Effects.Pipeline {
            val opponentDraws = DynamicAmounts.cardsDrawnThisTurn(Player.EachOpponent)
            val drawnBefore = storeNumber(opponentDraws)
            run(Effects.ForEachTarget(Effects.DrawCards(1, EffectTarget.ContextTarget(0))))
            run(Effects.AddCounters(CounterType.INGREDIENT, 1, EffectTarget.Self))
            run(Effects.AddDynamicCounters(CounterType.INGREDIENT, opponentDraws - drawnBefore.amount, EffectTarget.Self))
        }
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.countersOnSelf(CounterType.INGREDIENT),
            EffectTarget.TriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Andreia Ugrai"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acf6399f-f389-4b13-8563-a078a5d198f4.jpg?1783910728"
        ruling("2024-07-26", "You don't have to choose any target opponents for Communal Brewing's first ability. However, if you do, and all of those targets are illegal as the ability tries to resolve, it won't resolve and none of its effects will happen. No players will draw cards, and you won't put any ingredient counters on Communal Brewing.")
        ruling("2024-07-26", "The value of X is calculated as Communal Brewing's last ability resolves.")
        ruling("2024-07-26", "If Communal Brewing leaves the battlefield before its last ability resolves, use the number of ingredient counters that were on it as it last existed on the battlefield to determine the value of X.")
    }
}
