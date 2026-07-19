package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rat Out
 * {B}
 * Instant
 *
 * Up to one target creature gets -1/-1 until end of turn. You create a 1/1 black Rat creature
 * token with "This token can't block."
 *
 * "Up to one target" is an optional target: the -1/-1 only applies if a creature was chosen, but
 * the Rat token is always created regardless (the token creation doesn't depend on the target).
 */
val RatOut = card("Rat Out") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Up to one target creature gets -1/-1 until end of turn. " +
        "You create a 1/1 black Rat creature token with \"This token can't block.\""

    spell {
        val t = target("target", Targets.UpToCreatures(1))
        effect = Effects.Composite(
            Effects.ModifyStats(-1, -1, t),
            woeRatToken()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Michele Giorgi"
        flavorText = "\"Hold this for me.\"\n—Totentanz, swarm piper"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2c42755-bf91-4c75-95c6-d2a60ba3492a.jpg?1783915104"
    }
}
