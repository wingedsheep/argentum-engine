package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Valeron Wardens
 * {2}{G}
 * Creature — Human Monk
 * 1/3
 *
 * Renown 2 (When this creature deals combat damage to a player, if it isn't renowned, put two +1/+1 counters on it and it becomes renowned.)
 * Whenever a creature you control becomes renowned, draw a card.
 *
 * "A creature you control" includes Valeron Wardens itself — `Triggers.a(...)` binds ANY, source included.
 */
val ValeronWardens = card("Valeron Wardens") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Monk"
    oracleText = "Renown 2 (When this creature deals combat damage to a player, if it isn't renowned, put two +1/+1 counters on it and it becomes renowned.)\n" +
        "Whenever a creature you control becomes renowned, draw a card."
    power = 1
    toughness = 3

    keywordAbility(KeywordAbility.renown(2))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).becomesRenowned()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e693a7e7-d95e-4cd8-9524-ca5fc517189a.jpg?1783938316"
        ruling(
            "2015-06-22",
            "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger."
        )
    }
}
