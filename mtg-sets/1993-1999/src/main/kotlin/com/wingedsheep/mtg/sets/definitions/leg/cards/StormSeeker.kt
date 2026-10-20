package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Storm Seeker — Legends #205
 * {3}{G} · Instant
 *
 * Storm Seeker deals damage to target player equal to the number of cards in that player's hand.
 *
 * The hand count is read off the targeted player (`asPlayer`) when the spell resolves.
 */
val StormSeeker = card("Storm Seeker") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Storm Seeker deals damage to target player equal to the number of cards in that player's hand."

    spell {
        val player = target(Targets.Player)
        effect = Effects.DealDamage(
            DynamicAmounts.zone(player.asPlayer, Zone.HAND).count(),
            player,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b66d0cc-84d7-41ad-b0e7-74ebf604543f.jpg?1783948044"
    }
}
