package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charnel Troll
 * {1}{B}{G}
 * Creature — Troll
 * 4/4
 *
 * Trample
 * At the beginning of your upkeep, exile a creature card from your graveyard. If you do, put a
 * +1/+1 counter on this creature. Otherwise, sacrifice it.
 * {B}{G}, Discard a creature card: Put a +1/+1 counter on this creature.
 *
 * The exile is mandatory whenever a creature card is there (ruling), so it's a `chooseExactly(1)`
 * over the graveyard's creature cards — an empty pool chooses nothing. "If you do" branches on
 * [moveTracked]'s moved set, not the choice, so a card that never reaches exile counts as not
 * exiled and the Troll is sacrificed.
 */
val CharnelTroll = card("Charnel Troll") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Troll"
    oracleText = "Trample\n" +
        "At the beginning of your upkeep, exile a creature card from your graveyard. If you do, " +
        "put a +1/+1 counter on this creature. Otherwise, sacrifice it.\n" +
        "{B}{G}, Discard a creature card: Put a +1/+1 counter on this creature."
    power = 4
    toughness = 4

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val creatureCards = gather(
                CardSource.FromZone(zone = Zone.GRAVEYARD, filter = GameObjectFilter.Creature)
            )
            val chosen = chooseExactly(1, from = creatureCards, selectedLabel = "Exile")
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE))
            ifNotEmpty(exiled) {
                run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
            } orElse {
                run(Effects.SacrificeTarget(EffectTarget.Self))
            }
        }
        description = "At the beginning of your upkeep, exile a creature card from your graveyard. " +
            "If you do, put a +1/+1 counter on this creature. Otherwise, sacrifice it."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}{G}"), Costs.Discard(GameObjectFilter.Creature))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71fec229-e22c-4655-8640-02d0ec472be0.jpg?1783934139"
        ruling("2018-10-05", "You can't choose not to exile a creature card from your graveyard if you have one to exile.")
        ruling("2018-10-05", "You can activate Charnel Troll's last ability during your upkeep before its triggered ability resolves.")
    }
}
