package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Déjà Vu
 * {2}{U}
 * Sorcery
 * Return target sorcery card from your graveyard to your hand.
 */
val DejaVu = card("Déjà Vu") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        val t = target("target", TargetObject(
            filter = TargetFilter(GameObjectFilter.Sorcery.ownedByYou(), zone = Zone.GRAVEYARD)
        ))
        effect = MoveToZoneEffect(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Hannibal King"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c93d4e9-7fd6-4814-b86b-89b92d1dad3b.jpg"
    }
}
