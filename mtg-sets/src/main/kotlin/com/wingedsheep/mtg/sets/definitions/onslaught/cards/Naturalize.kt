package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Naturalize
 * {1}{G}
 * Instant
 * Destroy target artifact or enchantment.
 */
val Naturalize = card("Naturalize") {
    manaCost = "{1}{G}"
    typeLine = "Instant"

    spell {
        target = TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment))
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "275"
        artist = "Ron Spencer"
        flavorText = "\"Destroy what was never meant to be and you will learn the meaning of nature.\"\n—Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c9f1ef6-ee6e-46e7-8e0e-a5e42508a7a5.jpg?1562916856"
    }
}
