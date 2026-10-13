package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sierra, Nuka's Biggest Fan
 * {3}{W}
 * Legendary Creature — Human Citizen
 * 3/4
 *
 * The Nuka-Cola Challenge — Whenever one or more creatures you control deal combat damage to a
 * player, put a quest counter on Sierra and create a Food token.
 * Whenever you sacrifice a Food, target creature you control gets +X/+X until end of turn, where X
 * is the number of quest counters on Sierra.
 *
 * The first trigger is the batch combat-damage shape (Professional Face-Breaker). The second is
 * `Triggers.you.sacrifices` over any Food artifact — token or not, for any reason (rulings) — and X
 * is read when the pump resolves; if Sierra is gone by then, `Self` falls back to the counters she
 * left the battlefield with.
 */
val SierraNukasBiggestFan = card("Sierra, Nuka's Biggest Fan") {
    manaCost = "{3}{W}"
    typeLine = "Legendary Creature — Human Citizen"
    power = 3
    toughness = 4
    oracleText = "The Nuka-Cola Challenge — Whenever one or more creatures you control deal combat damage to a player, " +
        "put a quest counter on Sierra and create a Food token.\n" +
        "Whenever you sacrifice a Food, target creature you control gets +X/+X until end of turn, " +
        "where X is the number of quest counters on Sierra."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self) then Effects.CreateFood(1)
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact.withSubtype("Food"))
        val creature = target(TargetFilter.CreatureYouControl)
        val questCounters = DynamicAmounts.countersOnSelf(CounterType.QUEST)
        effect = Effects.ModifyStats(questCounters, questCounters, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/641dd97c-6577-4cab-8672-56f870477961.jpg?1783912415"
        ruling("2024-03-08", "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token. For example, if you sacrifice a nontoken Food artifact, such as Candy Trail from the Wilds of Eldraine set, Sierra's last ability will trigger.")
        ruling("2024-03-08", "Sierra's last ability triggers whenever you sacrifice a Food for any reason, not just to activate a Food's activated ability.")
    }
}
