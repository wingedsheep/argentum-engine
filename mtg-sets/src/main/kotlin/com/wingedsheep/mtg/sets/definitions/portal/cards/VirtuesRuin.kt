package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Virtue's Ruin
 * {2}{B}
 * Sorcery
 * Destroy all white creatures.
 */
val VirtuesRuin = card("Virtue's Ruin") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(
            GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE)),
            MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Adam Rex"
        flavorText = "Virtue crumbles before the relentless march of darkness."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7854928a-d467-4616-b96b-de7e5fe7303e.jpg"
    }
}
