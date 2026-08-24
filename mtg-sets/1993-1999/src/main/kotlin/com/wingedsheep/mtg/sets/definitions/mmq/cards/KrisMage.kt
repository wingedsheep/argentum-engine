package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kris Mage
 * {R}
 * Creature — Human Spellshaper
 * 1 / 1
 */
val KrisMage = card("Kris Mage") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{R}, {T}, Discard a card: This creature deals 1 damage to any target."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Matthew D. Wilson"
        flavorText = "Her blade draws blood without ever touching its target."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/4389fbcd-182a-4cac-b14f-aa971948cf8e.jpg"
    }
}
