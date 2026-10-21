package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Willow Geist — Innistrad: Midnight Hunt #207
 * {G} · Creature — Treefolk Spirit · 1/1
 *
 * Trample
 * Whenever one or more cards leave your graveyard, put a +1/+1 counter on this creature.
 * When this creature dies, you gain life equal to its power.
 *
 * The leave-graveyard trigger batches: it fires once per event batch however many cards left.
 * The dies trigger reads the creature's last-known power (counters included).
 */
val WillowGeist = card("Willow Geist") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk Spirit"
    power = 1
    toughness = 1
    oracleText = "Trample\n" +
        "Whenever one or more cards leave your graveyard, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, you gain life equal to its power."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).leaveYourGraveyard()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GainLife(DynamicAmounts.sourcePower())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Ryan Yee"
        flavorText = "Die beneath a tree, wake up a part of it."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c40c71c-4cdc-4a28-8ea3-4636c936f8aa.jpg?1783925568"
    }
}
