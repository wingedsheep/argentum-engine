package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Seismic Mage
 * {3}{R}
 * Creature — Human Spellshaper
 * 1 / 1
 */
val SeismicMage = card("Seismic Mage") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{2}{R}, {T}, Discard a card: Destroy target land."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", Targets.Land)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Pete Venters"
        flavorText = "The ground shakes when he walks. His customers shake if they're late with his fee."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9524432a-3186-4c7b-a780-28bdbe36053f.jpg"
    }
}
