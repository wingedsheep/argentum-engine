package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Roaming Ghostlight
 * {3}{U}{U}
 * Creature — Spirit
 * 3/2
 * Flying
 * When this creature enters, return up to one target non-Spirit creature to its owner's hand.
 */
val RoamingGhostlight = card("Roaming Ghostlight") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, return up to one target non-Spirit creature to its owner's hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.notSubtype(Subtype.SPIRIT), optional = true)
        effect = Effects.ReturnToHand(t)
        description = "When this creature enters, return up to one target non-Spirit creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Zezhou Chen"
        flavorText = "Some who die senseless deaths return as formless spirits, seeking answers where there are none."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cbac0e4-f79f-476d-b410-d19ab3696606.jpg?1783930723"
    }
}
