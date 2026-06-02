package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Lava Flow
 * {3}{R}{R}
 * Sorcery
 * Destroy target creature or land.
 */
val LavaFlow = card("Lava Flow") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.CreatureOrLandPermanent))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "John Coulthart"
        flavorText = "Nothing stands before the river of fire."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89e825e4-98be-49f0-bc5e-c8988118dcef.jpg"
    }
}
