package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Briar Hydra
 * {5}{G}
 * Creature — Plant Hydra
 * 6/6
 * Trample
 * Domain — Whenever this creature deals combat damage to a player, put X +1/+1 counters on
 * target creature you control, where X is the number of basic land types among lands you control.
 */
val BriarHydra = card("Briar Hydra") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Hydra"
    power = 6
    toughness = 6
    oracleText = "Trample\nDomain — Whenever this creature deals combat damage to a player, put X +1/+1 counters on target creature you control, where X is the number of basic land types among lands you control."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.domain(), creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "286"
        artist = "Caio Monteiro"
        flavorText = "Flora and fauna alike rose to Yavimaya's defense."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/390fd5d1-3ce9-456e-b130-30ad0557f4cf.jpg?1783921241"
    }
}
