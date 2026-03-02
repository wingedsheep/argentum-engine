package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Shatter
 * {1}{R}
 * Instant
 * Destroy target artifact.
 */
val Shatter = card("Shatter") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Destroy target artifact."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact)))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Zoltan Boros"
        flavorText = "The ogre's mind snapped. The bow was next. The archer followed quickly after."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c4ecaa0-a85f-4771-a829-1ab440b29165.jpg?1562784251"
    }
}
