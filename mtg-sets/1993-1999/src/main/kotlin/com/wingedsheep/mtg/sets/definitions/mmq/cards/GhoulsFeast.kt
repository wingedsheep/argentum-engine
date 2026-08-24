package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ghoul's Feast
 * {1}{B}
 * Instant
 * Target creature gets +X/+0 until end of turn, where X is the number of creature cards in your graveyard.
 */
val GhoulsFeast = card("Ghoul's Feast") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/+0 until end of turn, where X is the number of creature cards in your graveyard."

    spell {
        val creature = target("target", Targets.Creature)
        effect = Effects.ModifyStats(
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature),
            DynamicAmount.Fixed(0),
            creature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Alan Pollack"
        flavorText = "Mercadians not wealthy enough to buy a tomb are thrown into a bog called \"the Ghoul's Larder.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a0054c1-6510-41dd-8695-9bf50296b615.jpg"
    }
}
