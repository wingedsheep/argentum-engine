package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val OB_NIXILIS_EMBLEM = "Whenever a player draws a card, you lose 2 life."

/**
 * Ob Nixilis Reignited - {3}{B}{B}
 * Legendary Planeswalker — Nixilis
 * Starting Loyalty: 5
 *
 * +1: You draw a card and you lose 1 life.
 * −3: Destroy target creature.
 * −8: Target opponent gets an emblem with "Whenever a player draws a card, you lose 2 life."
 *
 * The emblem is owned and controlled by the targeted opponent (2015-08-25 ruling), so the −8 runs
 * the emblem creation inside `ForEachPlayer(Player.TargetOpponent, …)`, which rebinds "you" to that
 * opponent — the Chandra, Awakened Inferno shape with a targeted player instead of each opponent.
 * The emblem's "you lose 2 life" is then that opponent, and the emblem outlives Ob Nixilis and his
 * controller. Each card any player draws triggers it once.
 */
val ObNixilisReignited = card("Ob Nixilis Reignited") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Nixilis"
    startingLoyalty = 5
    oracleText = "+1: You draw a card and you lose 1 life.\n" +
        "−3: Destroy target creature.\n" +
        "−8: Target opponent gets an emblem with \"$OB_NIXILIS_EMBLEM\""

    // +1: You draw a card and you lose 1 life.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        description = "You draw a card and you lose 1 life."
    }

    // −3: Destroy target creature.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
        description = "Destroy target creature."
    }

    // −8: Target opponent gets an emblem with "Whenever a player draws a card, you lose 2 life."
    loyaltyAbility(-8) {
        target(Targets.Opponent)
        effect = Effects.ForEachPlayer(
            Player.TargetOpponent,
            Effects.CreateGlobalTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.anyPlayer.draws()
                    effect = Effects.LoseLife(2, EffectTarget.Controller)
                    description = OB_NIXILIS_EMBLEM
                },
                descriptionOverride = OB_NIXILIS_EMBLEM,
                emblem = true
            )
        )
        description = "Target opponent gets an emblem with \"$OB_NIXILIS_EMBLEM\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "119"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53b2d6e9-2a43-4ebe-9ae8-944375b46cce.jpg?1783938200"
        ruling(
            "2015-08-25",
            "The emblem created by Ob Nixilis Reignited's last ability is both owned and controlled by the " +
                "target opponent. In a multiplayer game, if you own Ob Nixilis Reignited and use him to give one " +
                "of your opponents an emblem, the emblem remains even if you leave the game.",
        )
    }
}
