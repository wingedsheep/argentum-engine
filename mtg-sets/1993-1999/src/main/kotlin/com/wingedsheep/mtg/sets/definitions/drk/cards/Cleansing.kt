package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cleansing
 * {W}{W}{W}
 * Sorcery
 * For each land, destroy that land unless any player pays 1 life.
 *
 * "For each land" is the iteration and "unless any player pays" is the per-land ransom, so this is
 * `ForEachInGroup` wrapping `UnlessAnyPlayerPays` rather than one board-wide question: every land
 * gets its own offer, in turn, and a player can save one and let the next go.
 *
 * *Any* player, not the land's controller — an opponent may buy your land back, or refuse to save
 * their own. The offer goes round in APNAP order and the first payer settles that land.
 *
 * Every land on the battlefield, both sides included; the sorcery is symmetrical.
 */
val Cleansing = card("Cleansing") {
    manaCost = "{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "For each land, destroy that land unless any player pays 1 life."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllLands,
            Effects.UnlessAnyPlayerPays(
                cost = Costs.pay.PayLife(1),
                effect = Effects.Destroy(EffectTarget.Self),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc1973a3-1410-4c6d-9b09-bd9d18646a1e.jpg?1783947949"
    }
}
