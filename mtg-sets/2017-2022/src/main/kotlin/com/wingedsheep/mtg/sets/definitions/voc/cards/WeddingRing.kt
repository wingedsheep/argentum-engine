package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wedding Ring — Innistrad: Crimson Vow Commander #32
 * {2}{W}{W} · Artifact · Mythic
 *
 * When this artifact enters, if it was cast, target opponent creates a token that's a copy of it.
 * Whenever an opponent who controls an artifact named Wedding Ring draws a card during their turn,
 * you draw a card.
 * Whenever an opponent who controls an artifact named Wedding Ring gains life during their turn,
 * you gain that much life.
 *
 * Modeling notes:
 *  - The ETB is an intervening "if it was cast" ([Conditions.WasCast]), so the opponent's token
 *    copy — which was never cast — doesn't hand a copy back. The copy reads last-known information
 *    if the Ring has already left by resolution.
 *  - "An opponent who controls an artifact named Wedding Ring … during their turn" is a CR 603.2
 *    trigger-event qualifier, not an intervening "if", so it is a `triggerRestriction` checked only
 *    when the draw / life gain happens: the triggering opponent controls *any* artifact named
 *    Wedding Ring (ruling) and it's that opponent's turn.
 */
private val opponentHasRingOnTheirTurn = Conditions.All(
    Exists(
        Player.TriggeringPlayer,
        Zone.BATTLEFIELD,
        GameObjectFilter.Artifact.named("Wedding Ring")
    ),
    Conditions.IsPlayersTurn(Player.TriggeringPlayer)
)

val WeddingRing = card("Wedding Ring") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, if it was cast, target opponent creates a token that's a copy of it.\n" +
        "Whenever an opponent who controls an artifact named Wedding Ring draws a card during their turn, you draw a card.\n" +
        "Whenever an opponent who controls an artifact named Wedding Ring gains life during their turn, you gain that much life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        val opponent = target(Targets.Opponent)
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.Self,
            controller = opponent,
        )
        description = "When this artifact enters, if it was cast, target opponent creates a token " +
            "that's a copy of it."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        triggerRestriction = opponentHasRingOnTheirTurn
        effect = Effects.DrawCards(1)
        description = "Whenever an opponent who controls an artifact named Wedding Ring draws a " +
            "card during their turn, you draw a card."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.gainsLife()
        triggerRestriction = opponentHasRingOnTheirTurn
        effect = Effects.GainLife(DynamicAmounts.triggerLifeGained())
        description = "Whenever an opponent who controls an artifact named Wedding Ring gains " +
            "life during their turn, you gain that much life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "32"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f0b1400-0608-47fd-9c73-b7730bcf6b7f.jpg?1783924997"
        ruling(
            "2021-11-19",
            "The trigger conditions of Wedding Ring's second and third abilities care about opponents who control any artifact named Wedding Ring, not just the token copy created by its first ability."
        )
    }
}
