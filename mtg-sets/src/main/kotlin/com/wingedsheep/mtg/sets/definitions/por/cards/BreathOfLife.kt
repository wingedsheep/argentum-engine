package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects

/**
 * Breath of Life
 * {3}{W}
 * Sorcery
 * Return target creature card from your graveyard to the battlefield.
 */
val BreathOfLife = card("Breath of Life") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"

    spell {
        val t = target("target", Targets.CreatureCardInYourGraveyard)
        effect = Effects.Move(t, Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcea5e09-6385-41df-970b-ac26c9b46127.jpg"
    }
}
