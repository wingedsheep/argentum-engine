package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Flashfires
 * {3}{R}
 * Sorcery
 * Destroy all Plains.
 */
val Flashfires = card("Flashfires") {
    manaCost = "{3}{R}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(
            GroupFilter(GameObjectFilter.Land.withSubtype(Subtype.PLAINS)),
            MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Dameon Willich"
        flavorText = "The prairies burn bright and fast."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9e88867-6acb-43f8-806b-21480aaa1afc.jpg"
    }
}
