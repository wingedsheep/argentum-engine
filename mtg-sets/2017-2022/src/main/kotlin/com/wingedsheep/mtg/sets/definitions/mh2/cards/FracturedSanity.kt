package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fractured Sanity
 * {U}{U}{U}
 * Sorcery
 * Each opponent mills fourteen cards.
 * Cycling {1}{U}
 * When you cycle this card, each opponent mills four cards.
 *
 * Both mills are the each-opponent mill recipe (Necromaster Dragon's player argument). The cycle
 * trigger is Renewed Faith's `Triggers.self.isCycled()`; it resolves before the cycling draw.
 */
val FracturedSanity = card("Fractured Sanity") {
    manaCost = "{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each opponent mills fourteen cards.\n" +
        "Cycling {1}{U} ({1}{U}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, each opponent mills four cards."

    spell {
        effect = Patterns.Library.mill(14, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    keywordAbility(KeywordAbility.cycling("{1}{U}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        effect = Patterns.Library.mill(4, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "When you cycle this card, each opponent mills four cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Drew Tucker"
        flavorText = "The body grows stronger under stress. The mind does not."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/413ac8fb-5156-4f90-81f5-1879e273c9e2.jpg?1783926878"
    }
}
