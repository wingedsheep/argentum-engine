package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vault 75: Middle School
 * {2}{W}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Exile all creatures with power 4 or greater.
 * II, III — Put a +1/+1 counter on each creature you control.
 *
 * Chapter I gathers every creature whose projected power is 4 or more and exiles them in one
 * simultaneous move (Sunfall's shape); chapters II and III are Genku's "+1/+1 counter on each
 * creature you control" group iteration.
 */
val Vault75MiddleSchool = card("Vault 75: Middle School") {
    manaCost = "{2}{W}{W}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Exile all creatures with power 4 or greater.\n" +
        "II, III — Put a +1/+1 counter on each creature you control."

    sagaChapter(1) {
        effect = Effects.Pipeline {
            val bigCreatures = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature.powerAtLeast(4)))
            move(bigCreatures, CardDestination.ToZone(Zone.EXILE))
        }
    }
    for (chapter in 2..3) {
        sagaChapter(chapter) {
            effect = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Victor Harmatiuk"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/3992e262-0491-4379-b74b-13cc8b63065e.jpg?1783912413"
    }
}
