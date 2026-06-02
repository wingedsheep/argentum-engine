package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Naturalize
 * {1}{G}
 * Instant
 * Destroy target artifact or enchantment.
 */
val Naturalize = card("Naturalize") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment)))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "275"
        artist = "Ron Spears"
        flavorText = "\"Destroy what was never meant to be and you will learn the meaning of nature.\"\n—Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0acc41f-b55b-47cb-8803-d39d72788799.jpg?1562940493"
    }
}
