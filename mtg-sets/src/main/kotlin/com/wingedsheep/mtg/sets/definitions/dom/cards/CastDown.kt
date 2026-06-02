package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.Effects

/**
 * Cast Down
 * {1}{B}
 * Instant
 * Destroy target nonlegendary creature.
 */
val CastDown = card("Cast Down") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target nonlegendary creature."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.NonlegendaryCreature))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Bastien L. Deharme"
        flavorText = "\"Your life is finished, your name lost, and your work forgotten. It is as though Mazeura never existed.\"\n—Chainer's Torment"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/116ce944-6871-4f51-a889-d9c4a5d7cff2.jpg?1591104775"
    }
}
