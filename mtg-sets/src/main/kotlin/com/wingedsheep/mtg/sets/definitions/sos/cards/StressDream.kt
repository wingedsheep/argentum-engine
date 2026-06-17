package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Stress Dream — Secrets of Strixhaven #235
 * {3}{U}{R} · Instant
 *
 * Stress Dream deals 5 damage to up to one target creature. Look at the top two cards of your
 * library. Put one of those cards into your hand and the other on the bottom of your library.
 *
 * The damage target is "up to one" (optional) — declining it is legal and the library look still
 * happens (Scryfall ruling). [Effects.DealDamage] no-ops on an empty optional target. The second
 * clause is [Patterns.Library.lookAtTopAndKeep]: look at the top two, keep one in hand, put the
 * rest on the bottom in the controller's chosen order ("in any order" → [CardOrder.ControllerChooses])
 * — which also covers the "only one card in library" ruling (you keep it), since the gather only
 * collects what's available.
 */
val StressDream = card("Stress Dream") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Stress Dream deals 5 damage to up to one target creature. Look at the top two " +
        "cards of your library. Put one of those cards into your hand and the other on the bottom " +
        "of your library."

    spell {
        val creature = target("up to one target creature", TargetCreature(optional = true))
        effect = Effects.DealDamage(5, creature)
            .then(
                Patterns.Library.lookAtTopAndKeep(
                    count = DynamicAmount.Fixed(2),
                    keepCount = DynamicAmount.Fixed(1),
                    keepDestination = CardDestination.ToZone(Zone.HAND),
                    restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    restOrder = CardOrder.ControllerChooses,
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "Krelg's attempt at a calming nap proved futile."
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ec40a1b-51e7-4a35-966c-ab2a10f21a80.jpg?1775938641"
    }
}
