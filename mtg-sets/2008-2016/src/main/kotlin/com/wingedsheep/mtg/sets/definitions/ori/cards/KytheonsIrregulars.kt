package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Kytheon's Irregulars
 * {2}{W}{W}
 * Creature — Human Soldier
 * 4/3
 * Renown 1
 * {W}{W}: Tap target creature.
 */
val KytheonsIrregulars = card("Kytheon's Irregulars") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 4
    toughness = 3
    oracleText = "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)\n{W}{W}: Tap target creature."

    keywordAbility(KeywordAbility.renown(1))

    activatedAbility {
        cost = Costs.Mana("{W}{W}")
        val t = target("target creature", Targets.Creature)
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Mark Winters"
        flavorText = "Kytheon and his irregulars worked outside the law to bring justice to the streets of Akros."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/440f9dbb-6d31-4c03-9c6d-c4910adc5497.jpg?1783938360"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
