package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fomori Vault
 * Land
 *
 * {T}: Add {C}.
 * {3}, {T}, Discard a card: Look at the top X cards of your library, where X is the number of
 * artifacts you control. Put one of those cards into your hand and the rest on the bottom of your
 * library in a random order.
 *
 * The dig is the standard look-at-top-and-keep pipeline ([Patterns.Library.lookAtTopAndKeep]):
 * gather the top X (where X = artifacts you control, via
 * [DynamicAmount.AggregateBattlefield] over [GameObjectFilter.Artifact] for [Player.You]),
 * keep one in hand, and bottom-randomize the rest. If X is 0 the pipeline gathers nothing and
 * the ability does nothing (matching "look at the top 0 cards").
 */
val FomoriVault = card("Fomori Vault") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{3}, {T}, Discard a card: Look at the top X cards of your library, where X is the number " +
        "of artifacts you control. Put one of those cards into your hand and the rest on the bottom " +
        "of your library in a random order."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.DiscardCard)
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Artifact),
            keepCount = DynamicAmount.Fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
        description = "Look at the top X cards of your library, where X is the number of artifacts " +
            "you control. Put one of those cards into your hand and the rest on the bottom of your " +
            "library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "29"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5433b98-4657-4bbe-9e72-d3c94c6aa8ef.jpg?1739804247"
    }
}
