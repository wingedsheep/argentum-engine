package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget

/**
 * Reminisce
 * {2}{U}
 * Sorcery
 * Target player shuffles their graveyard into their library.
 */
val Reminisce = card("Reminisce") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"
    oracleText = "Target player shuffles their graveyard into their library."

    spell {
        target = Targets.Player
        effect = Effects.ShuffleGraveyardIntoLibrary(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Bradley Williams"
        flavorText = "\"Leave the door to the past even slightly ajar and it could be blown off its hinges.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5f246e3-2193-4820-9c59-07b480300fbe.jpg?1562937866"
    }
}
