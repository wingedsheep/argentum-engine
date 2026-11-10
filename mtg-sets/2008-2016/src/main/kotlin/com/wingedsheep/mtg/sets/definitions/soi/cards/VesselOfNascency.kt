package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Vessel of Nascency (Shadows over Innistrad #237)
 * {G}
 * Enchantment
 *
 * {1}{G}, Sacrifice this enchantment: Reveal the top four cards of your library. You may put an
 * artifact, creature, enchantment, land, or planeswalker card from among them into your hand.
 * Put the rest into your graveyard.
 *
 * The five types are spelled out rather than read as "permanent card": the Oracle text still
 * lists them, so a battle card revealed this way is not eligible.
 */
val VesselOfNascency = card("Vessel of Nascency") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{1}{G}, Sacrifice this enchantment: Reveal the top four cards of your library. You may put an artifact, creature, enchantment, land, or planeswalker card from among them into your hand. Put the rest into your graveyard."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.SacrificeSelf)
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(4),
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.IsArtifact,
                            CardPredicate.IsCreature,
                            CardPredicate.IsEnchantment,
                            CardPredicate.IsLand,
                            CardPredicate.IsPlaneswalker,
                        )
                    )
                )
            ),
            prompt = "You may put an artifact, creature, enchantment, land, or planeswalker card from among them into your hand",
            revealed = true,
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "237"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89533790-38c2-4b53-90fa-8abf8c1a6abb.jpg?1783937718"
    }
}
