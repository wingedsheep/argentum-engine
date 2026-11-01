package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter


/**
 * Laquatus's Disdain
 * {1}{U}
 * Instant
 * Counter target spell cast from a graveyard.
 * Draw a card.
 */
val LaquatussDisdain = card("Laquatus's Disdain") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell cast from a graveyard.\nDraw a card."
    spell {
        target(TargetFilter(GameObjectFilter.Any.cast(Zone.GRAVEYARD), zone = Zone.STACK))
        effect = Effects.CounterSpell() then Effects.DrawCards(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Pete Venters"
        flavorText = "\"The first time was amusing, but now you're getting tiresome.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2ea5448-2d72-42eb-814c-197153d8e06a.jpg"
    }
}
