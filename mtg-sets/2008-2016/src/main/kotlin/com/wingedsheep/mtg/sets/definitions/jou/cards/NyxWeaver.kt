package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nyx Weaver
 * {1}{B}{G}
 * Enchantment Creature — Spider
 * 2/3
 * Reach
 * At the beginning of your upkeep, mill two cards.
 * {1}{B}{G}, Exile this creature: Return target card from your graveyard to your hand.
 */
val NyxWeaver = card("Nyx Weaver") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment Creature — Spider"
    oracleText = "Reach\n" +
        "At the beginning of your upkeep, mill two cards. (Put the top two cards of your library into your graveyard.)\n" +
        "{1}{B}{G}, Exile this creature: Return target card from your graveyard to your hand."
    power = 2
    toughness = 3

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Library.mill(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}{G}"), Costs.ExileSelf)
        val t = target(TargetFilter.CardInGraveyard.ownedByYou())
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e8553cc-8f61-4022-97c9-e31183b65059.jpg?1783939403"
    }
}
