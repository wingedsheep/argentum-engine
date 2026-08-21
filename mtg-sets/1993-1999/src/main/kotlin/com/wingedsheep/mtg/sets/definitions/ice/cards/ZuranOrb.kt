package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Zuran Orb — Ice Age #350
 * {0} · Artifact
 *
 * Sacrifice a land: You gain 2 life.
 *
 * The whole card is one activated ability with no mana component: `Costs.Sacrifice` over
 * [GameObjectFilter.Land] is the entire cost. `Sacrifice` rather than `SacrificeAnother` — Zuran Orb
 * is an artifact, so it can never match a land filter, and the printed line places no "another"
 * restriction. Repeatability is intentional: with no tap symbol and no once-per-turn rider the
 * ability can be activated as many times as there are lands to feed it, which is exactly why the
 * card is what it is.
 *
 * Overgrown Estate is the same shape at three life.
 */
val ZuranOrb = card("Zuran Orb") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Sacrifice a land: You gain 2 life."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Land)
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "350"
        artist = "Sandra Everingham"
        flavorText = "\"I will go to any length to achieve my goal. Eternal life is worth any sacrifice.\"\n—Zur the Enchanter"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a9d1082-a862-45d4-9e5e-392e879fead6.jpg?1783947454"
    }
}
