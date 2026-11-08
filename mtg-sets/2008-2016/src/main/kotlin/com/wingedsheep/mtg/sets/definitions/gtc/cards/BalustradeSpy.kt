package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Balustrade Spy — Gatecrash #57 (canonical printing)
 * {3}{B} · Creature — Vampire Rogue · 2/3
 *
 * Flying
 * When this creature enters, target player reveals cards from the top of their library until
 * they reveal a land card, then puts those cards into their graveyard.
 *
 * The enters trigger is Consuming Aberration's GatherUntilMatch + RevealCollection +
 * MoveCollection pipeline aimed at the target player (Tunnel Vision's `Player.TargetPlayer`
 * walk). The revealed pile includes the land, so the whole pile is binned; with no land in the
 * library the walk runs the library out and every card goes to the graveyard, per the rulings.
 */
val BalustradeSpy = card("Balustrade Spy") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, target player reveals cards from the top of their library " +
        "until they reveal a land card, then puts those cards into their graveyard."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(Targets.Player)
        effect = Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(GameObjectFilter.Land, player = Player.TargetPlayer)
            reveal(allRevealed)
            toGraveyard(allRevealed, Player.TargetPlayer)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df8a3f05-864d-401d-a2f1-5f58358fe089.jpg?1783940132"
        ruling(
            "2024-01-12",
            "The land card they reveal, if any, is included in the cards put into their graveyard."
        )
        ruling(
            "2017-11-17",
            "If the target player has no land cards in their library, all cards from that library " +
                "will be revealed and put into their graveyard."
        )
    }
}
