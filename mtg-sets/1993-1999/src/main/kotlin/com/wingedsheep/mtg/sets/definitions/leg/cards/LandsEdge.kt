package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Land's Edge
 * {1}{R}{R}
 * World Enchantment
 *
 * Discard a card: If the discarded card was a land card, this enchantment deals 2 damage to
 * target player or planeswalker. Any player may activate this ability.
 *
 * The activating player discards from their own hand and chooses the target; the enchantment is
 * the damage source. The target is chosen on activation whatever is discarded — the land check
 * reads the discarded card (`EffectTarget.DiscardedAsCost`) at resolution.
 */
val LandsEdge = card("Land's Edge") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "World Enchantment"
    oracleText = "Discard a card: If the discarded card was a land card, this enchantment deals 2 damage to " +
        "target player or planeswalker. Any player may activate this ability."

    activatedAbility {
        cost = Costs.Discard()
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.EntityMatches(EffectTarget.DiscardedAsCost(), GameObjectFilter.Land),
            then = Effects.DealDamage(2, victim),
        )
        restrictions = listOf(ActivationRestriction.AnyPlayerMay)
        description = "Discard a card: If it was a land card, deal 2 damage to target player or planeswalker."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb3d9b29-948c-4768-b5ea-db2512817c30.jpg?1783948053"
        ruling(
            "2004-10-04",
            "The damage done when you discard a land only applies to lands which are discarded by choice " +
                "using the Land's Edge ability. It does not work on forced discards of any type.",
        )
    }
}
