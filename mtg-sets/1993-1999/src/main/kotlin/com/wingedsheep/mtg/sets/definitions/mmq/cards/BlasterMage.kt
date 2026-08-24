package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Blaster Mage
 * {2}{R}
 * Creature — Human Spellshaper
 * 2 / 2
 * {R}, {T}, Discard a card: Destroy target Wall.
 *
 * "Target Wall" is a bare tribal noun — any *permanent* with the Wall subtype, not
 * `GameObjectFilter.Creature` (Dwarven Demolition Team uses the same filter).
 */
val BlasterMage = card("Blaster Mage") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{R}, {T}, Discard a card: Destroy target Wall."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype("Wall"))))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "George Pratt"
        flavorText = "\"Don't get up. I'll show myself out.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/801b0fd1-bbb2-47c0-a4c3-4129a67473b9.jpg"
    }
}
