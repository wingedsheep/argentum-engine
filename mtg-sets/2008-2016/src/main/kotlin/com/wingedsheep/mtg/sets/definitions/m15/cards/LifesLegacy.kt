package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Life's Legacy — Magic 2015 #183 (canonical printing)
 * {1}{G} · Sorcery
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Draw cards equal to the sacrificed creature's power.
 *
 * The sacrificed creature is gone by resolution, so `DynamicAmounts.sacrificedPower()` reads its
 * last-known power — the projected power snapshotted as the cost was paid, pumps and counters
 * included.
 */
val LifesLegacy = card("Life's Legacy") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Draw cards equal to the sacrificed creature's power."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = Effects.DrawCards(DynamicAmounts.sacrificedPower())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Howard Lyon"
        flavorText = "At the instant of death, the mystery of life."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d964629-6d1d-4253-ac23-92f2199323b4.jpg?1783939165"
        ruling("2014-07-18", "You must sacrifice exactly one creature to cast this spell; you can't cast it without sacrificing a creature, and you can't sacrifice additional creatures.")
        ruling("2014-07-18", "Players can only respond once this spell has been cast and all its costs have been paid. No one can try to destroy the creature you sacrificed to prevent you from casting this spell.")
    }
}
