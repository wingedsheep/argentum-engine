package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Saddleback Lagac — Oath of the Gatewatch #142 (canonical printing)
 * {3}{G} · Creature — Lizard · 3/1
 *
 * When this creature enters, support 2. (Put a +1/+1 counter on each of up to two other target creatures.)
 *
 * Support on a permanent targets *other* creatures (any controller): Expedition Raptor's ETB shape.
 */
val SaddlebackLagac = card("Saddleback Lagac") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, support 2. (Put a +1/+1 counter on each of up to two " +
        "other target creatures.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, support 2 — put a +1/+1 counter on each of up " +
            "to two other target creatures."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Jason Kang"
        flavorText = "\"A good lagac will carry you through thick and thin. A bad one . . . well, it's a " +
            "tasty dinner.\"\n—Raff Slugeater, goblin shortcutter"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33df4803-b32d-43ae-ac68-b72e45e10b0c.jpg?1783937899"
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
