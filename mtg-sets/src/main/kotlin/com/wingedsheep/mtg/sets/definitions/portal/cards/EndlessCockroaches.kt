package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.OnDeath
import com.wingedsheep.sdk.scripting.Zone

/**
 * Endless Cockroaches
 * {1}{B}{B}
 * Creature — Insect
 * 1/1
 * When Endless Cockroaches dies, return it to its owner's hand.
 */
val EndlessCockroaches = card("Endless Cockroaches") {
    manaCost = "{1}{B}{B}"
    typeLine = "Creature — Insect"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = OnDeath()
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.Hand)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d3d18b9-ad59-435b-934b-703e10287e32.jpg"
    }
}
