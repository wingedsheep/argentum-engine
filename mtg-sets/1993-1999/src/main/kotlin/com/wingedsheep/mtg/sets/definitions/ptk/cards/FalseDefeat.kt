package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * False Defeat
 * {3}{W}
 * Sorcery
 *
 * Return target creature card from your graveyard to the battlefield.
 */
val FalseDefeat = card("False Defeat") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield."

    spell {
        val creature = target("target", TargetObject(filter = TargetFilter.CreatureInYourGraveyard))
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Li Wang"
        flavorText = "\"All warfare is based on deception.\"\n—Sun Tzu, *Art of War* (trans. Giles)"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5ca71ce3-8633-428a-8d9e-9b807b77a8e2.jpg"
    }
}
