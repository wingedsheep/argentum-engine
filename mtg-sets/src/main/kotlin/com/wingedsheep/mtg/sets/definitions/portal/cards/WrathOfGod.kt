package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Wrath of God
 * {2}{W}{W}
 * Sorcery
 * Destroy all creatures. They can't be regenerated.
 */
val WrathOfGod = card("Wrath of God") {
    manaCost = "{2}{W}{W}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures, MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true), noRegenerate = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Mike Raabe"
        flavorText = "\"'Twas said they died, not that they were dead.\"\n—Matthew Prior"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d75d8204-6f9d-4a7a-bb8b-d51ac65a30fa.jpg"
    }
}
