package com.wingedsheep.mtg.sets.definitions.brotherswar.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul Partition
 * {1}{W}
 * Instant
 * Exile target nonland permanent. For as long as that card remains exiled, its owner
 * may play it. A spell cast by an opponent this way costs {2} more to cast.
 */
val SoulPartition = card("Soul Partition") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Exile target nonland permanent. For as long as that card remains exiled, its owner may play it. A spell cast by an opponent this way costs {2} more to cast."

    spell {
        val permanent = target("target nonland permanent", Targets.NonlandPermanent)
        effect = Effects.ExileAndGrantOwnerPlayPermission(
            target = permanent,
            opponentCostIncrease = 2
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Kekai Kotaki"
        flavorText = "Teferi's body couldn't travel through time far enough, but with Kaya's help, his spirit could."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28bb8ec0-9729-4aa1-8ce4-a3a5598b0d70.jpg?1674420320"
    }
}
