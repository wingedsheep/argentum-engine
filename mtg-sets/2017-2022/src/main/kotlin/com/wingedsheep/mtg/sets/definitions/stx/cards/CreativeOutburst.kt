package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Creative Outburst — Strixhaven: School of Mages #171 (canonical printing)
 * {3}{U}{U}{R}{R} · Instant
 *
 * Creative Outburst deals 5 damage to any target. Look at the top five cards of your library. Put
 * one of them into your hand and the rest on the bottom of your library in a random order.
 * {U/R}{U/R}, Discard this card: Create a Treasure token.
 *
 * The spell is damage from the spell itself ([Targets.Any], `damageSource = Self`) followed by the
 * Shimmer of Possibility [Patterns.Library.lookAtTopAndKeep] pipeline with a random bottom order.
 * The second line is an activated ability that works only from hand (`activateFromZone =
 * Zone.HAND`), its discard paid as [Costs.DiscardSelf] — the Saiba Trespassers channel shape.
 */
val CreativeOutburst = card("Creative Outburst") {
    manaCost = "{3}{U}{U}{R}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Creative Outburst deals 5 damage to any target. Look at the top five cards of " +
        "your library. Put one of them into your hand and the rest on the bottom of your library " +
        "in a random order.\n" +
        "{U/R}{U/R}, Discard this card: Create a Treasure token."

    spell {
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(5, anyTarget, damageSource = EffectTarget.Self) then
            Patterns.Library.lookAtTopAndKeep(
                count = 5,
                keepCount = 1,
                restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                restOrder = CardOrder.Random
            )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/R}{U/R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Effects.CreateTreasure()
        description = "{U/R}{U/R}, Discard this card: Create a Treasure token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eab58d87-bf01-45dc-8958-e2b3375f914b.jpg?1783927322"
    }
}
