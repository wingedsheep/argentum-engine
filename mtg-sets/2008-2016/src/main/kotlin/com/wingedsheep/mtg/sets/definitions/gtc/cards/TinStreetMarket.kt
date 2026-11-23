package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tin Street Market
 * {4}{R}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}, Discard a card: Draw a card."
 */
val TinStreetMarket = card("Tin Street Market") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}, Discard a card: Draw a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Composite(Costs.Tap, Costs.DiscardCard),
                effect = Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Noah Bradley"
        flavorText = "\"Sometimes I stroll along Tin Street and watch citizens busy with their daily lives. " +
            "It reminds me why I do my job.\"\n—Arrester Lavinia, Tenth Precinct"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c1f543b-2222-4ef5-b4f7-2c3d2ea27fdc.jpg?1783940121"
    }
}
