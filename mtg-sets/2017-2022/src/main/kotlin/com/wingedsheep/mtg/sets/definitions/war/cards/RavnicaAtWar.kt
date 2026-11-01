package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Ravnica at War — War of the Spark #28 (canonical printing)
 * {3}{W}
 * Sorcery
 * Exile all multicolored permanents.
 *
 * The same gather-then-exile pipeline Urza's Ruinous Blast uses, over permanents whose projected
 * colors number two or more ([GameObjectFilter.Multicolored]).
 */
val RavnicaAtWar = card("Ravnica at War") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile all multicolored permanents."

    spell {
        effect = Effects.Pipeline {
            val multicolored = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Permanent and GameObjectFilter.Multicolored
                )
            )
            exile(multicolored)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Adam Paquette"
        flavorText = "The heart of Ravnica disappeared before anyone could strike a blow in its defense."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0f83824-43c6-4101-88fd-9109958b23e2.jpg?1783933477"
    }
}
