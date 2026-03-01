package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

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
        val t = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment)))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "James Paick"
        flavorText = "The remains of ancient sky tyrants now feed the war-torn land."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b129b44e-a1ce-41f2-a0cf-b6c879c7cbbd.jpg?1562792044"
    }
}
