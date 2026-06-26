package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Peer Past the Veil
 * {2}{R}{G}
 * Instant
 * Discard your hand. Then draw X cards, where X is the number of card types among cards in your graveyard.
 *
 * The discard and draw resolve in sequence within the [Effects.Composite]; the discarded cards reach the
 * graveyard before X is evaluated, so they contribute their card types to the count (CR 608.2: the spell's
 * instructions are followed in order). X reads [DynamicAmount.AggregateZone] over the controller's graveyard
 * with [Aggregation.DISTINCT_TYPES] — the number of distinct card types (artifact, creature, enchantment,
 * instant, land, planeswalker, sorcery, …) among those cards.
 */
val PeerPastTheVeil = card("Peer Past the Veil") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Discard your hand. Then draw X cards, where X is the number of card types among cards in your graveyard."

    spell {
        effect = Effects.Composite(
            Patterns.Hand.discardHand(EffectTarget.Controller),
            Effects.DrawCards(
                DynamicAmount.AggregateZone(
                    player = Player.You,
                    zone = Zone.GRAVEYARD,
                    aggregation = Aggregation.DISTINCT_TYPES
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/874eadb6-602e-47dc-8094-82e37ac89c94.jpg?1726286712"
    }
}
