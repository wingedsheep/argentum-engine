package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tataru Taru
 * {1}{W}
 * Legendary Creature — Dwarf Advisor
 * 0/3
 * When Tataru Taru enters, you draw a card and target opponent may draw a card.
 * Scions' Secretary — Whenever an opponent draws a card, if it isn't that player's turn, create a
 * tapped Treasure token. This ability triggers only once each turn.
 *
 * The enters trigger targets the opponent, so an illegal target fizzles the whole ability and
 * you don't draw either (2025-06-06 ruling). The opponent's "may" is theirs to decide —
 * [Effects.May] with `decisionMaker` set to the targeted opponent.
 *
 * "If it isn't that player's turn" is an intervening-if on the drawing (triggering) player's
 * turn, as on Scytheclaw Raptor. Draws on the opponent's own turn never trigger, so they don't
 * spend the once-each-turn cap (`oncePerTurn`).
 */
val TataruTaru = card("Tataru Taru") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dwarf Advisor"
    oracleText = "When Tataru Taru enters, you draw a card and target opponent may draw a card.\n" +
        "Scions' Secretary — Whenever an opponent draws a card, if it isn't that player's turn, " +
        "create a tapped Treasure token. This ability triggers only once each turn."
    power = 0
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.DrawCards(1) then
            Effects.May(
                effect = Effects.DrawCards(1, opponent),
                decisionMaker = opponent,
                descriptionOverride = "Draw a card?",
            )
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        interveningIf = Conditions.Not(Conditions.IsPlayersTurn(Player.TriggeringPlayer))
        effect = Effects.CreateTreasure(tapped = true)
        oncePerTurn = true
        description = "Scions' Secretary — Whenever an opponent draws a card, if it isn't that " +
            "player's turn, create a tapped Treasure token. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93176e85-2343-43a8-8578-69699b464b86.jpg?1783906365"
        ruling(
            "2025-06-06",
            "If the target opponent is an illegal target as Tataru Taru's first ability tries to " +
                "resolve, it won't resolve and none of its effects will happen. You won't draw a card."
        )
    }
}
