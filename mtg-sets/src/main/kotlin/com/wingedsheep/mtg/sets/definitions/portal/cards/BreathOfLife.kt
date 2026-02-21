package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone

/**
 * Breath of Life
 * {3}{W}
 * Sorcery
 * Return target creature card from your graveyard to the battlefield.
 */
val BreathOfLife = card("Breath of Life") {
    manaCost = "{3}{W}"
    typeLine = "Sorcery"

    spell {
        val t = target("target", Targets.CreatureCardInYourGraveyard)
        effect = MoveToZoneEffect(t, Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcea5e09-6385-41df-970b-ac26c9b46127.jpg"
    }
}
