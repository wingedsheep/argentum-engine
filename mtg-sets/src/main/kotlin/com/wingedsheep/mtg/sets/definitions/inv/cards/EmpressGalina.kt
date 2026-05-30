package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Empress Galina
 * {3}{U}{U}
 * Legendary Creature — Merfolk Noble
 * 1/3
 * {U}{U}, {T}: Gain control of target legendary permanent. (This effect lasts indefinitely.)
 *
 * GainControlEffect defaults to Duration.Permanent, matching "lasts indefinitely".
 */
val EmpressGalina = card("Empress Galina") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Merfolk Noble"
    power = 1
    toughness = 3
    oracleText = "{U}{U}, {T}: Gain control of target legendary permanent. (This effect lasts indefinitely.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}{U}"), Costs.Tap)
        val t = target(
            "target",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.legendary()))
        )
        effect = GainControlEffect(t)
        description = "{U}{U}, {T}: Gain control of target legendary permanent."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6851dbc7-f072-41e7-a899-897445d99425.jpg?1562916018"
    }
}
