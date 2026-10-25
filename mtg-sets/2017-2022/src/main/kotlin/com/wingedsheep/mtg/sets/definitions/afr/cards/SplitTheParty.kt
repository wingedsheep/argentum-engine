package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Split the Party
 * {3}{U}{U}
 * Sorcery
 * Choose target player. Return half the creatures they control to their owner's hand, rounded up.
 *
 * Only the player is targeted — the creatures are not, so hexproof/ward creatures can be returned.
 * On resolution the creatures the target player controls are gathered, and the spell's controller
 * (per the ruling) chooses exactly half of them, rounded up — the count read off the gathered
 * collection itself. A permanent leaving the battlefield always goes to its owner's hand.
 */
val SplitTheParty = card("Split the Party") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose target player. Return half the creatures they control to their owner's hand, rounded up."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.ControlledPermanents(player.asPlayer, GameObjectFilter.Creature))
            val toReturn = chooseExactly(
                creatures.count divRoundedUp 2,
                from = creatures,
                chooser = Chooser.Controller,
                useTargetingUI = true,
                prompt = "Choose half the creatures they control, rounded up, to return to their owner's hand"
            )
            move(toReturn, CardDestination.ToZone(Zone.HAND))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Zoltan Boros"
        flavorText = "Don't."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/743a0a96-f90a-4852-9a92-848795518e0a.jpg?1783926506"
        ruling("2021-07-23", "The player that controls Split the Party chooses which creatures to return to their owner's hand as it resolves.")
    }
}
