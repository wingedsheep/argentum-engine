package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Siren of the Silent Song
 * {1}{U}{B}
 * Creature — Zombie Siren
 * 2/1
 *
 * Flying
 * Inspired — Whenever this creature becomes untapped, each opponent discards a card, then each
 * opponent mills a card.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val SirenOfTheSilentSong = card("Siren of the Silent Song") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Siren"
    power = 2
    toughness = 1
    oracleText = "Flying\nInspired — Whenever this creature becomes untapped, each opponent discards a " +
        "card, then each opponent mills a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.EachOpponentDiscards(1) then
            Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/990f4438-aa74-4460-8bac-93480d0bb0dc.jpg?1783939521"
    }
}
