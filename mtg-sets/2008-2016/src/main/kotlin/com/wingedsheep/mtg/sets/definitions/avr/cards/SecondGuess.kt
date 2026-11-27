package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Second Guess
 * {1}{U}
 * Instant
 * Counter target spell that's the second spell cast this turn.
 *
 * The cast order counts every player's spells; `nthSpellCastThisTurn(2)` reads the position the
 * engine stamps on each spell as it is cast, so a copy (never cast) is never a legal target.
 */
val SecondGuess = card("Second Guess") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell that's the second spell cast this turn."

    spell {
        target(TargetFilter.SpellOnStack.nthSpellCastThisTurn(2))
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Karl Kopinski"
        flavorText = "\"I see where you're going with that. I don't like it.\"\n—Dierk, geistmage"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d22d093-8e89-4d54-ac04-14c8759de3ea.jpg?1783940711"
    }
}
