package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Fulminator Mage
 * {1}{B/R}{B/R}
 * Creature — Elemental Shaman
 * 2 / 2
 *
 * Sacrifice this creature: Destroy target nonbasic land.
 *
 * - The whole cost is the sacrifice — no {T}, so the ability works the turn the Mage enters and
 *   works while it is tapped or attacking.
 * - [TargetFilter.NonbasicLand] is `IsLand` plus `Not(IsBasicLand)`, so a land that is basic by
 *   type (a Snow-Covered basic, or a nonbasic land turned into a basic type by a continuous
 *   effect) is not a legal target — the check reads projected state.
 * - [Effects.Destroy] lowers to a move-to-graveyard by destruction, so an indestructible or
 *   regenerating land survives.
 */
val FulminatorMage = card("Fulminator Mage") {
    manaCost = "{1}{B/R}{B/R}"
    typeLine = "Creature — Elemental Shaman"
    power = 2
    toughness = 2
    oracleText = "Sacrifice this creature: Destroy target nonbasic land."

    activatedAbility {
        cost = Costs.SacrificeSelf
        val t = target("target", TargetPermanent(filter = TargetFilter.NonbasicLand))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "rk post"
        flavorText = "\"Unsafe Terrain Ahead—Turn Back\"\n" +
            "—Sign near the former location of Pyrtagh Cairn"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be1df367-8e85-4fd8-aa6f-f02a478fecb3.jpg?1783942726"
    }
}
