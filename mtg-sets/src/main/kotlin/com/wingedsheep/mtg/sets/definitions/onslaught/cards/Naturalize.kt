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
    oracleText = "Destroy target artifact or enchantment."

    spell {
        target = TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment))
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "275"
        artist = "Ron Spears"
        flavorText = "\"Destroy what was never meant to be and you will learn the meaning of nature.\"\n—Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/large/front/c/0/c0acc41f-b55b-47cb-8803-d39d72788799.jpg?1562940493"
    }
}
