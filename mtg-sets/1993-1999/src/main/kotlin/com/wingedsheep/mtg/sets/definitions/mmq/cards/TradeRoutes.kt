package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Trade Routes
 * {1}{U}
 * Enchantment
 * {1}: Return target land you control to its owner's hand.
 * {1}, Discard a land card: Draw a card.
 *
 * Two independent activated abilities. The bounce targets a land you already control, so the
 * "its owner's hand" wording is just the engine's default destination for a zone change to
 * [com.wingedsheep.sdk.core.Zone.HAND]. The second ability's discard is a typed cost atom
 * ([Costs.Discard] over [GameObjectFilter.Land]), not an effect.
 */
val TradeRoutes = card("Trade Routes") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "{1}: Return target land you control to its owner's hand.\n" +
        "{1}, Discard a land card: Draw a card."

    activatedAbility {
        cost = Costs.Mana("{1}")
        val land = target("target", TargetPermanent(filter = TargetFilter.Land.youControl()))
        effect = Effects.ReturnToHand(land)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Discard(GameObjectFilter.Land))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Matt Cavotta"
        flavorText = "Like the price of goods, the value of the routes was renegotiated daily."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeaba189-b215-4d1c-9135-a86ce5ec955d.jpg"
    }
}
