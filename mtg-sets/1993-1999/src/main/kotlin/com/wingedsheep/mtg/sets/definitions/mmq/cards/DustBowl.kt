package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Dust Bowl
 *
 * Land
 *
 * {T}: Add {C}.
 * {3}, {T}, Sacrifice a land: Destroy target nonbasic land.
 *
 * The sacrifice is a plain [Costs.Sacrifice] over [GameObjectFilter.Land] — "a land", not
 * "another land", so Dust Bowl itself is a legal sacrifice. The destroy ability is not a mana
 * ability (it targets), so only the first ability carries `manaAbility`, which also settles its
 * timing.
 */
val DustBowl = card("Dust Bowl") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{3}, {T}, Sacrifice a land: Destroy target nonbasic land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Land)
        )
        val land = target("target", TargetPermanent(filter = TargetFilter.NonbasicLand))
        effect = Effects.Destroy(land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "316"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75b03c30-c2b8-4207-b675-26c59c40a7e5.jpg"
    }
}
