package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Gitrog, Ravenous Ride
 * {3}{B}{G}
 * Legendary Creature — Frog Horror Mount
 * 6/5
 *
 * Trample, haste
 * Whenever The Gitrog deals combat damage to a player, you may sacrifice a creature that saddled
 * it this turn. If you do, draw X cards, then put up to X land cards from your hand onto the
 * battlefield tapped, where X is the sacrificed creature's power.
 * Saddle 1
 *
 * Implementation:
 * - Trample/haste keywords + Saddle 1 ([KeywordAbility.saddle]).
 * - The combat-damage trigger resolves an inline [Effects.Pipeline]: it gathers the creatures
 *   that saddled The Gitrog this turn ([CardSource.CreaturesThatSaddledSource], the same
 *   source-relative record Fortune, Loyal Steed reads), the controller picks **up to one** to
 *   sacrifice ("you may"), and the pipeline sacrifices it. Picking none makes the sacrifice — and
 *   therefore the draw/put — a no-op, which is exactly the "If you do" gating.
 * - X is the sacrificed creature's power, read via [DynamicAmounts.sacrificedPower] off the LKI
 *   snapshot the sacrifice records (Rule 608.2h). Draw X, then put up to X land cards from hand
 *   onto the battlefield tapped (gather hand lands → choose up to X → move to battlefield tapped).
 */
val TheGitrogRavenousRide = card("The Gitrog, Ravenous Ride") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Frog Horror Mount"
    power = 6
    toughness = 5
    oracleText = "Trample, haste\n" +
        "Whenever The Gitrog deals combat damage to a player, you may sacrifice a creature that " +
        "saddled it this turn. If you do, draw X cards, then put up to X land cards from your hand " +
        "onto the battlefield tapped, where X is the sacrificed creature's power.\n" +
        "Saddle 1"

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    keywordAbility(KeywordAbility.saddle(1))

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Pipeline {
            // You may sacrifice a creature that saddled The Gitrog this turn.
            val saddlers = gather(CardSource.CreaturesThatSaddledSource)
            val chosen = chooseUpTo(
                1,
                from = saddlers,
                useTargetingUI = true,
                prompt = "You may sacrifice a creature that saddled The Gitrog this turn",
                selectedLabel = "Sacrifice",
            )
            sacrifice(chosen)
            // If you do, draw X, then put up to X land cards from hand tapped — X = sacrificed power.
            run(Effects.DrawCards(DynamicAmounts.sacrificedPower()))
            val handLands = gather(
                CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Land)
            )
            val lands = chooseUpTo(
                DynamicAmounts.sacrificedPower(),
                from = handLands,
                prompt = "Put up to X land cards from your hand onto the battlefield tapped",
            )
            move(
                lands,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped),
            )
        }
        description = "Whenever The Gitrog deals combat damage to a player, you may sacrifice a " +
            "creature that saddled it this turn. If you do, draw X cards, then put up to X land " +
            "cards from your hand onto the battlefield tapped, where X is the sacrificed creature's power."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "206"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82512813-8618-483b-a7f0-e6a611d9d487.jpg?1712356103"

        ruling("2024-04-12", "X is determined by the power of the sacrificed creature as it last existed on the battlefield.")
        ruling("2024-04-12", "You choose which creature to sacrifice (if any) and then draw and put lands all while the triggered ability resolves.")
        ruling("2024-04-12", "If you don't sacrifice a creature, you don't draw cards or put lands onto the battlefield.")
    }
}
