package com.wingedsheep.mtg.sets.definitions.dsc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Persistent Constrictor {4}{B} — Creature — Zombie Snake 5/3
 *
 * At the beginning of each opponent's upkeep, they lose 1 life and you put a -1/-1 counter on up
 * to one target creature they control.
 * Persist
 *
 * "They" is the player whose upkeep it is (the triggering player); the target is scoped to that
 * player's creatures the same way Tooth Collector scopes its Delirium trigger.
 */
val PersistentConstrictor = card("Persistent Constrictor") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Snake"
    power = 5
    toughness = 3
    oracleText = "At the beginning of each opponent's upkeep, they lose 1 life and you put a -1/-1 " +
        "counter on up to one target creature they control.\n" +
        "Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the " +
        "battlefield under its owner's control with a -1/-1 counter on it.)"

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer()),
            optional = true,
        )
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature)
        description = "At the beginning of each opponent's upkeep, they lose 1 life and you put a " +
            "-1/-1 counter on up to one target creature they control."
    }

    keywords(Keyword.PERSIST)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Steve Ellis"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a25d478-84fd-4e4b-abb7-f5f6becf6eb0.jpg?1783909658"
        ruling("2024-09-20", "If the target creature is an illegal target as Persistent Constrictor's first ability tries to resolve, it won't resolve and none of its effects will happen. No player will lose life.")
    }
}
