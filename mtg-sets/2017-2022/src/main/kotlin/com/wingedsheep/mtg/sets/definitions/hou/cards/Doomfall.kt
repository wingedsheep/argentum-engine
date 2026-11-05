package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Doomfall
 * {2}{B}
 * Sorcery
 *
 * Choose one —
 * • Target opponent exiles a creature they control.
 * • Target opponent reveals their hand. You choose a nonland card from it. Exile that card.
 *
 * Mode one mirrors Debt to the Kami: the opponent is the target and picks which of their own
 * creatures to exile, so hexproof on the creature is irrelevant. Mode two mirrors Check for Traps:
 * the caster picks a nonland card from the revealed hand; a hand of only lands exiles nothing.
 */
val Doomfall = card("Doomfall") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n• Target opponent exiles a creature they control.\n" +
        "• Target opponent reveals their hand. You choose a nonland card from it. Exile that card."

    spell {
        modal(chooseCount = 1) {
            mode("Target opponent exiles a creature they control") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    val creatures = gather(
                        CardSource.FromZone(Zone.BATTLEFIELD, opponent.asPlayer, GameObjectFilter.Creature)
                    )
                    val chosen = chooseExactly(
                        1, from = creatures,
                        chooser = Chooser.TargetPlayer,
                        prompt = "Choose a creature to exile"
                    )
                    exile(chosen, owner = opponent.asPlayer)
                }
            }
            mode("Target opponent reveals their hand. You choose a nonland card from it. Exile that card") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    run(Effects.RevealHand(opponent))
                    val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                    val chosen = chooseExactly(
                        1,
                        from = opponentHand,
                        chooser = Chooser.Controller,
                        filter = GameObjectFilter.Nonland,
                        prompt = "Choose a nonland card to exile",
                        alwaysPrompt = true,
                        showAllCards = true
                    )
                    exile(chosen, owner = opponent.asPlayer)
                }
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Darek Zabrocki"
        flavorText = "\"This place has outlived its usefulness.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17db6ef5-1aa9-4b8c-b0b7-d6668a2347a1.jpg?1783936041"
        ruling(
            "2017-07-14",
            "If you choose Doomfall's first mode, the target opponent chooses which creature to exile. " +
                "That creature isn't targeted, so a creature with hexproof can be exiled this way."
        )
        ruling(
            "2017-07-14",
            "If you choose Doomfall's second mode, you must exile a nonland card from that player's hand if " +
                "able. If you can't, most likely because that player's hand contains only land cards, nothing " +
                "happens. That player won't exile a creature they control instead."
        )
    }
}
