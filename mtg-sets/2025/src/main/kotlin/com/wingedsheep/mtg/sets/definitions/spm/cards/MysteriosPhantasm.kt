package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mysterio's Phantasm
 * {1}{U}
 * Creature — Illusion Villain
 * 1/3
 * Flying, vigilance
 * Whenever this creature attacks, mill a card. (Put the top card of your library into your graveyard.)
 */
val MysteriosPhantasm = card("Mysterio's Phantasm") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion Villain"
    oracleText = "Flying, vigilance\nWhenever this creature attacks, mill a card. (Put the top card of your library into your graveyard.)"
    power = 1
    toughness = 3
    keywords(Keyword.FLYING, Keyword.VIGILANCE)
    triggeredAbility {
        trigger = Triggers.self.attacks()
        // The published recipe, not a hand-built pipeline: `Patterns.Library.mill` stamps
        // `isMill = true` on the gather, which is what CR 701.17's "mills that many cards plus
        // four instead" replacements key off. The hand-rolled version lost that flag.
        effect = Patterns.Library.mill(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Piotr Dura"
        flavorText = "Mysterio's greatest talent was the ability to instill doubt in one's own senses."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79aa0a78-80a2-44be-8a79-92bcba9c040f.jpg?1757376980"
    }
}
