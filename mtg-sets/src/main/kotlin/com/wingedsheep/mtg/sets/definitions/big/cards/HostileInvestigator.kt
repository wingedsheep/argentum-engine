package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hostile Investigator
 * {3}{B}
 * Creature — Ogre Rogue Detective
 * 4/3
 *
 * When this creature enters, target opponent discards a card.
 * Whenever one or more players discard one or more cards, investigate. This
 * ability triggers only once each turn. (Create a Clue token. It's an artifact
 * with "{2}, Sacrifice this token: Draw a card.")
 */
val HostileInvestigator = card("Hostile Investigator") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Rogue Detective"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, target opponent discards a card.\n" +
        "Whenever one or more players discard one or more cards, investigate. This ability triggers only once each turn. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    // ETB: target opponent discards a card.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", Targets.Opponent)
        effect = Patterns.Hand.discardCards(1, opponent)
    }

    // "Whenever one or more players discard one or more cards, investigate." The
    // batch ("one or more") collapses to a single fire because oncePerTurn caps
    // the ability at one Clue per turn regardless of how many discards happen.
    triggeredAbility {
        trigger = Triggers.discards(player = Player.Each)
        effect = Effects.Investigate()
        oncePerTurn = true
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/158c000e-7960-4518-b034-a529622b7bf1.jpg?1739804181"
    }
}
