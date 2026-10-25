package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Demogorgon's Clutches
 * {2}{B}
 * Sorcery
 *
 * Target opponent discards two cards, mills two cards, and loses 2 life.
 */
val DemogorgonsClutches = card("Demogorgon's Clutches") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards, mills two cards, and loses 2 life. " +
        "(To mill a card, a player puts the top card of their library into their graveyard.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(2, opponent) then
            Patterns.Library.mill(2, opponent) then
            Effects.LoseLife(2, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Alexander Mokhov"
        flavorText = "The good will be corrupted, the kind will be devoured, and all will be dragged into the bottomless Abyss."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/581e7ac6-65e5-4a16-ab0b-0988c3a3cfea.jpg?1783926498"
    }
}
