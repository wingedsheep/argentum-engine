package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Renewing Touch
 * {G}
 * Sorcery
 * Shuffle any number of target creature cards from your graveyard into your library.
 */
val RenewingTouch = card("Renewing Touch") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Shuffle any number of target creature cards from your graveyard into your library."

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, unlimited = true)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            move(chosen, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Rebecca Guay"
        flavorText = "Death just encourages life the more."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b63ec869-f933-4b27-9f0b-b583e71a1110.jpg?1783946450"
    }
}
