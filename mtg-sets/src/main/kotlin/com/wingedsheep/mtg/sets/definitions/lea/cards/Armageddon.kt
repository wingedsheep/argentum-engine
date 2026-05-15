package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Armageddon
 * {3}{W}
 * Sorcery
 * Destroy all lands.
 */
val Armageddon = card("Armageddon") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllLands, MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b6ddce7-b9c5-431d-a0b0-46d4aa93cbcb.jpg?1559591324"
    }
}
