package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Wipe Clean
 * {1}{W}
 * Instant
 * Exile target enchantment.
 * Cycling {3}
 */
val WipeClean = card("Wipe Clean") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Exile target enchantment.\nCycling {3}"

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Enchantment)))
        effect = MoveToZoneEffect(t, Zone.EXILE)
    }

    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Arnie Swekel"
        flavorText = "The light of the Ancestor will scour the taint of darkness from the land."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf7c14c9-cb5a-49f0-be2c-eb3166f02510.jpg?1562534791"
    }
}
