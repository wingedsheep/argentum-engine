package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Galvanic Bombardment
 * {R}
 * Instant
 * Galvanic Bombardment deals X damage to target creature, where X is 2 plus the number of cards
 * named Galvanic Bombardment in your graveyard.
 *
 * X is counted on resolution, while this spell is still on the stack, so it never counts itself —
 * the same `n + count(NameEquals)` shape as Ancestral Anger and Goblin Gathering.
 */
val GalvanicBombardment = card("Galvanic Bombardment") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Galvanic Bombardment deals X damage to target creature, where X is 2 plus the " +
        "number of cards named Galvanic Bombardment in your graveyard."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            2 + DynamicAmounts.count(
                Player.You,
                Zone.GRAVEYARD,
                GameObjectFilter(cardPredicates = listOf(CardPredicate.NameEquals("Galvanic Bombardment"))),
            ),
            t,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Terese Nielsen"
        flavorText = "Geist-powered devices have no shortage of fuel."
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc6a4367-bbee-43b8-b5dd-28498f8e2ede.jpg?1783937459"
    }
}
