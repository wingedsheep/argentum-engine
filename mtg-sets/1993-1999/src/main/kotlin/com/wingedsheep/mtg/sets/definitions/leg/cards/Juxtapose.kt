package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Juxtapose — Legends #63
 * {3}{U} · Sorcery
 *
 * You and target player exchange control of the creature you each control with the greatest mana
 * value. Then exchange control of artifacts the same way. If two or more permanents a player
 * controls are tied for greatest, their controller chooses one of them.
 *
 * Each exchange gathers its side's permanents *at that moment*, narrows them to the ones tied for
 * the greatest mana value, and lets that side's controller break a tie (you with
 * [Chooser.Controller], the target player with [Chooser.TargetPlayer]). The artifact gather runs
 * after the creature exchange, so an artifact creature that just changed hands counts for its new
 * controller (the 2007 ruling). Nothing is targeted but the player, so shroud doesn't stop it. If a
 * side has nothing to offer, its pick is empty, the exchange step fails and is skipped, and the
 * artifact exchange still happens.
 */
val Juxtapose = card("Juxtapose") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "You and target player exchange control of the creature you each control with the greatest " +
        "mana value. Then exchange control of artifacts the same way. If two or more permanents a player " +
        "controls are tied for greatest, their controller chooses one of them."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            for ((kind, noun) in listOf(GameObjectFilter.Creature to "creature", GameObjectFilter.Artifact to "artifact")) {
                val yours = chooseExactly(
                    1,
                    from = filter(
                        gather(CardSource.ControlledPermanents(Player.You, kind)),
                        CollectionFilter.GreatestManaValue
                    ),
                    chooser = Chooser.Controller,
                    prompt = "Choose your $noun with the greatest mana value to exchange",
                    useTargetingUI = true
                )
                val theirs = chooseExactly(
                    1,
                    from = filter(
                        gather(CardSource.ControlledPermanents(player.asPlayer, kind)),
                        CollectionFilter.GreatestManaValue
                    ),
                    chooser = Chooser.TargetPlayer,
                    prompt = "Choose your $noun with the greatest mana value to exchange",
                    useTargetingUI = true
                )
                run(Effects.ExchangeControl(yours.asTarget, theirs.asTarget))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Justin Hampton"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c48d78db-d982-4c28-9308-2d57dc2b947e.jpg?1783948074"
        ruling(
            "2007-09-16",
            "First creatures are exchanged, then artifacts are exchanged. It's possible that the same " +
                "artifact creature may be involved in both exchanges."
        )
        ruling(
            "2007-09-16",
            "No permanents are targeted by this effect, so permanents with Shroud may be exchanged this way."
        )
        ruling(
            "2007-09-16",
            "If one of the players doesn't control a creature at the time the exchange would be made, that " +
                "part of the effect does nothing, but the exchange of artifacts will still happen. Similarly, if " +
                "one of the players doesn't control an artifact at the time the exchange would be made, that " +
                "part of the effect does nothing."
        )
    }
}

