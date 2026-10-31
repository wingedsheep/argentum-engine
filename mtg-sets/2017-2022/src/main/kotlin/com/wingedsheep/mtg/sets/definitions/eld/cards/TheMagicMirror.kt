package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Magic Mirror — Throne of Eldraine #51
 * {6}{U}{U}{U} · Legendary Artifact · Mythic
 *
 * This spell costs {1} less to cast for each instant and sorcery card in your graveyard.
 * You have no maximum hand size.
 * At the beginning of your upkeep, put a knowledge counter on The Magic Mirror, then draw a card
 * for each knowledge counter on The Magic Mirror.
 *
 * The draw count reads the Mirror live and falls back to its last-known counters if it left the
 * battlefield in response (ruling 2019-10-04).
 */
val TheMagicMirror = card("The Magic Mirror") {
    manaCost = "{6}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact"
    oracleText = "This spell costs {1} less to cast for each instant and sorcery card in your graveyard.\n" +
        "You have no maximum hand size.\n" +
        "At the beginning of your upkeep, put a knowledge counter on The Magic Mirror, then draw a card " +
        "for each knowledge counter on The Magic Mirror."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(filter = GameObjectFilter.InstantOrSorcery),
            ),
        )
    }

    staticAbility {
        ability = NoMaximumHandSize()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.KNOWLEDGE, 1, EffectTarget.Self) then
            Effects.DrawCards(DynamicAmounts.countersOnSelf(CounterType.KNOWLEDGE))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "51"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08b89af1-7b22-4153-b42d-a2ea4e0f320c.jpg?1783932655"
        ruling("2019-10-04", "Adventurer cards aren't instant or sorcery cards while they're in your graveyard.")
        ruling("2019-10-04", "If a split card is both an instant card and a sorcery card, it's only counted once for The Magic Mirror's cost reduction ability.")
        ruling("2019-10-04", "If The Magic Mirror leaves the battlefield while its last ability is on the stack, the ability will use the number of knowledge counters The Magic Mirror had before leaving the battlefield to determine how many cards you'll draw.")
        ruling("2019-10-04", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2019-10-04", "The cost reduction ability reduces only the generic mana in the relic's cost. The colored mana must still be paid.")
        ruling("2019-10-04", "Once you announce that you're casting a spell, no player may take actions until the spell has been paid for. Notably, opponents can't try to change by how much a relic's cost is reduced.")
    }
}
