package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Inspired Idea
 * {2}{U}
 * Sorcery
 * Cleave {3}{U}{U} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Draw three cards. [Your maximum hand size is reduced by three for the rest of the game.]
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast draws three but permanently lowers your maximum hand size by three; paying the
 * cleave cost drops that downside entirely.
 *
 * Effect-only difference (the spell has no target): the base [SpellBuilder.effect] draws three
 * and applies the rest-of-game reduction via [Effects.ReduceMaximumHandSize] (a one-shot effect
 * that confers an accumulating, player-scoped reduction independent of this spell — see
 * [com.wingedsheep.engine.core.MaximumHandSize]); the [SpellBuilder.cleaveEffect] just draws
 * three. The engine picks the cleave effect when the spell is cast for its cleave cost.
 */
val InspiredIdea = card("Inspired Idea") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Cleave {3}{U}{U} (You may cast this spell for its cleave cost. If you do, " +
        "remove the words in square brackets.)\n" +
        "Draw three cards. [Your maximum hand size is reduced by three for the rest of the game.]"

    keywordAbility(KeywordAbility.cleave("{3}{U}{U}"))

    spell {
        // Printed (brackets present): draw three, then reduce your maximum hand size by three
        // for the rest of the game.
        effect = Effects.DrawCards(3).then(Effects.ReduceMaximumHandSize(3))

        // Cleaved (brackets removed): draw three, no downside.
        cleaveEffect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Alix Branwyn"
        flavorText = "Stitching is an ever-evolving field, with each new advancement reaching a " +
            "new height of horror."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fe4ee8e-579d-4bfa-8c19-bfdb1c0b7177.jpg?1783924891"
    }
}
