package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vaevictis Asmadi, the Dire — Core Set 2019 #225
 * {3}{B}{R}{G} · Legendary Creature — Elder Dragon · 6/6
 *
 * Flying
 * Whenever Vaevictis Asmadi attacks, for each player, choose target permanent that player
 * controls. Those players sacrifice those permanents. Each player who sacrificed a permanent this
 * way reveals the top card of their library, then puts it onto the battlefield if it's a permanent
 * card.
 *
 * "For each player, choose target permanent that player controls" is the one-per-player
 * distribution with no "up to": exactly one target per player (`exactly = PlayerCount(Each)` +
 * `differentControllers`), so a player with no permanents leaves the trigger without enough legal
 * targets and it is removed from the stack (ruling), and you must pick one of your own.
 *
 * The resolution is Builder's Bane's shape: capture each still-legal target's controller, sacrifice
 * them all in one simultaneous move (so a permanent revealed later can't touch another player's
 * sacrifice), then for each player who actually sacrificed — a target that became illegal is gone
 * from the chosen targets, and a player protected from being made to sacrifice keeps theirs —
 * reveal the top card of that player's library and put it onto the battlefield if it's a
 * permanent card, in turn order.
 */
val VaevictisAsmadiTheDire = card("Vaevictis Asmadi, the Dire") {
    manaCost = "{3}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Whenever Vaevictis Asmadi attacks, for each player, choose target permanent that player " +
        "controls. Those players sacrifice those permanents. Each player who sacrificed a permanent " +
        "this way reveals the top card of their library, then puts it onto the battlefield if it's a " +
        "permanent card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        targets(
            TargetFilter.Permanent,
            exactly = DynamicAmounts.playerCount(Player.Each),
            differentControllers = true,
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val controllers = captureControllers(chosen)
            val sacrificed = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Sacrifice,
            )
            forEachCaptured(sacrificed, original = chosen, controllers = controllers) {
                val top = gather(CardSource.TopOfLibrary(count = 1, player = Player.You), revealed = true)
                val permanentCard = selectAll(from = top, filter = GameObjectFilter.Permanent)
                move(permanentCard, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            }
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "225"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d19fdc00-21eb-48dc-966a-6b634dc5a2c4.jpg?1783934517"
        ruling("2018-07-13", "You must target one of your permanents to sacrifice as well.")
        ruling(
            "2018-07-13",
            "Vaevictis's triggered ability resolves during the declare attackers step, before blockers " +
                "are chosen. A creature targeted by its ability won't be around to block, and creatures put " +
                "onto the battlefield with its ability may be able to do so."
        )
        ruling(
            "2018-07-13",
            "If any of the target permanents aren't sacrificed (most likely because one of them became an " +
                "illegal target, such as by gaining hexproof), that permanent's controller won't reveal the " +
                "top card of their library or put it onto the battlefield."
        )
        ruling(
            "2018-07-13",
            "If any player controls no permanents, Vaevictis's ability won't have sufficient legal targets, " +
                "so it's removed from the stack without any effect."
        )
    }
}
