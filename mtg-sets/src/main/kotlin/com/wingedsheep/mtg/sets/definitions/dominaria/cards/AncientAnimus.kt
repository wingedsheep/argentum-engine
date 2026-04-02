package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Ancient Animus
 * {1}{G}
 * Instant
 * Put a +1/+1 counter on target creature you control if it's legendary.
 * Then it fights target creature an opponent controls.
 */
val AncientAnimus = card("Ancient Animus") {
    manaCost = "{1}{G}"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control if it's legendary. Then it fights target creature an opponent controls."

    spell {
        val yourCreature = target("creature you control", TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.youControl())
        ))
        val theirCreature = target("creature an opponent controls", TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.opponentControls())
        ))
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.legendary()),
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, yourCreature)
        ).then(Effects.Fight(yourCreature, theirCreature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Titus Lunter"
        flavorText = "Multani's mind grasped for consciousness as rage itself rebuilt his body."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6a3b223-b232-4da3-9431-ccb4688d5941.jpg?1562741668"
        ruling("2020-11-10", "If either target is an illegal target as Ancient Animus tries to resolve, neither creature will deal or be dealt damage.")
        ruling("2020-11-10", "If the creature you control is an illegal target as Ancient Animus tries to resolve, you won't put a +1/+1 counter on it. If that creature is a legal target but the creature you don't control isn't, you'll still put the counter on the creature you control if it's legendary.")
    }
}
