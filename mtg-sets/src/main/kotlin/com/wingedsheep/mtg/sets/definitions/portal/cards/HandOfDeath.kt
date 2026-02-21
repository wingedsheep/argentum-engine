package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Hand of Death
 * {2}{B}
 * Sorcery
 * Destroy target nonblack creature.
 */
val HandOfDeath = card("Hand of Death") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Brian Snoddy"
        flavorText = "Reach out your hand and touch the dead."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27f136b8-52be-49b9-919b-2b9785254350.jpg"
    }
}
