package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReturnFromGraveyardEffect
import com.wingedsheep.sdk.scripting.SearchDestination

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
        target = Targets.CreatureCardInYourGraveyard
        effect = ReturnFromGraveyardEffect(
            unifiedFilter = GameObjectFilter.Creature,
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcea5e09-6385-41df-970b-ac26c9b46127.jpg"
    }
}
